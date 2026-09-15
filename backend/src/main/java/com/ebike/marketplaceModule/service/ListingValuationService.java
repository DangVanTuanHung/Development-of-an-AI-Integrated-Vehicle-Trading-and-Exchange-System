package com.ebike.marketplaceModule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ListingValuationService {
    public record Group(String key, String label, String pattern, String fuel, int mileageTolerance) {}
    public record Observation(String id, String group, int year, String condition, BigDecimal price,
        Integer mileageKm, String title, String sourceName, String sourceUrl, LocalDate sourceDate,
        LocalDate collectedOn, String captureMethod, String priceType) {}
    public record Dataset(int schemaVersion, List<Group> groups, List<Observation> observations) {}
    public record Vehicle(String title, Integer year, String condition, String fuel, Integer mileage) {}
    public record Valuation(String status, BigDecimal low, BigDecimal high, String confidence,
        String explanation, String source, LocalDate estimatedAt, int sampleCount, String matchedModel,
        List<Observation> references) {}

    private final MarketplaceListingService listings;
    private final ObjectMapper mapper;
    private final Resource data;
    public ListingValuationService(MarketplaceListingService listings, ObjectMapper mapper,
        @Value("${app.market-prices.resource:classpath:market-data/prices.json}") Resource data) {
        this.listings = listings; this.mapper = mapper; this.data = data;
    }

    public Valuation estimate(UUID id) {
        var listing = listings.detail(id);
        if (!"PUBLISHED".equals(listing.status())) return unavailable("Tin chưa được công khai.", List.of(), null);
        try (var stream = data.getInputStream()) {
            Dataset dataset = mapper.readValue(stream, Dataset.class);
            return calculate(new Vehicle(listing.title(), listing.manufactureYear(), listing.condition(),
                listing.fuelType(), listing.mileageKm()), dataset, LocalDate.now(ZoneOffset.UTC));
        } catch (java.io.IOException ex) {
            return unavailable("Chưa đọc được dữ liệu giá tham khảo. Vui lòng thử lại sau.", List.of(), null);
        }
    }

    static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").replace('đ', 'd').replaceAll("\\s+", " ").trim();
    }

    Valuation calculate(Vehicle vehicle, Dataset dataset, LocalDate today) {
        String title = normalize(vehicle.title());
        List<Group> matches = dataset.groups().stream().filter(g -> Pattern.compile(g.pattern()).matcher(title).find()).toList();
        if (matches.size() != 1 || title.contains(" / ") || title.contains(" hoac "))
            return unavailable("Chưa có nguồn đối chiếu cho đúng dòng/phiên bản xe này.", List.of(), null);
        Group group = matches.get(0);
        boolean dieselTitle = Pattern.compile("\\b(dau|diesel|2[ .]?2d)\\b").matcher(title).find();
        boolean petrolTitle = Pattern.compile("\\b(xang|gasoline)\\b").matcher(title).find();
        boolean hybridTitle = Pattern.compile("\\b(hybrid|hev)\\b").matcher(title).find();
        if (("santafe-diesel".equals(group.key()) && !"DIESEL".equals(vehicle.fuel()) && !dieselTitle)
            || (dieselTitle && !"DIESEL".equals(group.fuel()))
            || (petrolTitle && "DIESEL".equals(group.fuel()))
            || (hybridTitle && !"HYBRID".equals(group.fuel())))
            return unavailable("Cần xác nhận nhiên liệu và phiên bản để đối chiếu đúng nhóm xe.", List.of(), group.label());

        Set<Integer> titleYears = new HashSet<>();
        var matcher = Pattern.compile("\\b(19[8-9][0-9]|20[0-9]{2})\\b").matcher(title);
        while (matcher.find()) titleYears.add(Integer.parseInt(matcher.group()));
        Integer year = vehicle.year();
        if (year == null && titleYears.size() == 1) year = titleYears.iterator().next();
        if (year == null || titleYears.size() > 1 || (!titleYears.isEmpty() && !titleYears.contains(year)))
            return unavailable("Cần xác nhận năm sản xuất để so sánh đúng đời xe.", List.of(), group.label());
        if (vehicle.fuel() != null && !vehicle.fuel().isBlank() && !group.fuel().equals(vehicle.fuel()))
            return unavailable("Nhiên liệu khai báo chưa khớp dòng xe. Cần xác nhận lại thông tin.", List.of(), group.label());
        String condition = Set.of("USED", "LIKE_NEW").contains(vehicle.condition()) ? "USED" : vehicle.condition();
        int targetYear = year;
        Map<String, Observation> unique = new LinkedHashMap<>();
        for (Observation row : dataset.observations()) {
            if (!group.key().equals(row.group()) || row.year() != targetYear || !Objects.equals(condition, row.condition())
                || !"ASKING_FULL_PRICE".equals(row.priceType()) || row.price() == null || row.price().signum() <= 0
                || row.sourceDate() == null || row.sourceDate().isBefore(today.minusDays(90)) || row.sourceDate().isAfter(today)
                || row.collectedOn() == null || row.collectedOn().isAfter(today) || row.id() == null
                || !safeUrl(row.sourceUrl())) continue;
            if (vehicle.mileage() != null && row.mileageKm() != null
                && Math.abs((long)vehicle.mileage() - row.mileageKm()) > group.mileageTolerance()) continue;
            unique.putIfAbsent(row.id(), row);
        }
        List<Observation> rows = new ArrayList<>(unique.values());
        rows.sort(Comparator.comparing(Observation::price));
        if (rows.size() >= 4) {
            BigDecimal q1 = quantile(rows, .25), q3 = quantile(rows, .75);
            BigDecimal spread = q3.subtract(q1).multiply(new BigDecimal("1.5"));
            BigDecimal median = quantile(rows, .5);
            rows.removeIf(row -> row.price().compareTo(q1.subtract(spread)) < 0 || row.price().compareTo(q3.add(spread)) > 0
                || row.price().compareTo(median.multiply(new BigDecimal("0.5"))) < 0 || row.price().compareTo(median.multiply(new BigDecimal("2"))) > 0);
        }
        if (rows.size() < 3) return unavailable("Chưa đủ 3 mẫu giá phù hợp. Các nguồn tìm được hiển thị bên dưới; chưa tính khoảng giá.", rows, group.label());
        BigDecimal low = quantile(rows, .25).setScale(0, RoundingMode.HALF_UP);
        BigDecimal high = quantile(rows, .75).setScale(0, RoundingMode.HALF_UP);
        LocalDate newest = rows.stream().map(Observation::sourceDate).max(LocalDate::compareTo).orElse(today);
        return new Valuation("AVAILABLE", low, high, "LOW",
            "Khoảng giữa 50% giá rao đã thu thập, cùng dòng và đời xe; tách xe mới/đã sử dụng. Chưa hiệu chỉnh theo khu vực, phụ kiện hoặc kiểm định thực tế.",
            "EXTERNAL_ASKING_PRICES", newest, rows.size(), group.label(), List.copyOf(rows));
    }

    static boolean safeUrl(String url) {
        try { var uri = java.net.URI.create(url); return "https".equals(uri.getScheme()) && "xe.chotot.com".equals(uri.getHost()); }
        catch (Exception ignored) { return false; }
    }
    static BigDecimal quantile(List<Observation> sorted, double p) {
        double index = (sorted.size() - 1) * p;
        int base = (int) index;
        BigDecimal start = sorted.get(base).price();
        BigDecimal end = sorted.get(Math.min(base + 1, sorted.size() - 1)).price();
        return start.add(end.subtract(start).multiply(BigDecimal.valueOf(index - base)));
    }
    private Valuation unavailable(String text, List<Observation> references, String model) {
        LocalDate latest = references.stream().map(Observation::sourceDate).max(LocalDate::compareTo).orElse(null);
        return new Valuation("INSUFFICIENT_DATA", null, null, "LOW", text, "EXTERNAL_ASKING_PRICES", latest,
            references.size(), model, List.copyOf(references));
    }
}
