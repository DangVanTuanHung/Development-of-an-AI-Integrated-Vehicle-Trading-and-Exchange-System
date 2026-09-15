package com.ebike.marketplaceModule.service;

import com.ebike.authModule.entity.User;
import com.ebike.authModule.repository.UserRepository;
import com.ebike.marketplaceModule.dto.CreateListingRequest;
import com.ebike.marketplaceModule.dto.ListingResponse;
import com.ebike.marketplaceModule.entity.VehicleListing;
import com.ebike.marketplaceModule.repository.VehicleListingRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MarketplaceListingService {
    private static final java.util.Set<String> LISTING_TYPES = java.util.Set.of("SALE", "EXCHANGE", "SALE_OR_EXCHANGE");
    private static final java.util.Set<String> CONDITIONS = java.util.Set.of("NEW", "LIKE_NEW", "USED", "RESTORED", "DAMAGED");
    private final VehicleListingRepository listingRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Path storageRoot;
    private final long maxImageBytes;

    public MarketplaceListingService(VehicleListingRepository listingRepository, UserRepository userRepository, JdbcTemplate jdbcTemplate,
                                     @Value("${app.marketplace.storage.root}") String storageRoot,
                                     @Value("${app.marketplace.storage.max-image-bytes}") long maxImageBytes) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.maxImageBytes = maxImageBytes;
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> publicListings() {
        return listingRepository.findByStatusOrderByPublishedAtDesc("PUBLISHED").stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> ownListings(String username) {
        return listingRepository.findBySellerUsernameOrderByCreatedAtDesc(username).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ListingResponse detail(UUID publicId) {
        return listingRepository.findByPublicId(publicId).map(this::toResponse)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
    }

    @Transactional
    public ListingResponse create(String username, CreateListingRequest request) {
        User seller = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        Integer categoryCount = jdbcTemplate.queryForObject("select count(*) from marketplace.vehicle_categories where id = ? and active", Integer.class, request.categoryId());
        if (categoryCount == null || categoryCount == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category");
        String listingType = request.listingType().trim().toUpperCase(Locale.ROOT);
        String condition = request.condition().trim().toUpperCase(Locale.ROOT);
        if (!LISTING_TYPES.contains(listingType)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid listing type");
        if (!CONDITIONS.contains(condition)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid condition");

        VehicleListing listing = new VehicleListing();
        listing.setSeller(seller);
        listing.setCategoryId(request.categoryId());
        listing.setBrandId(request.brandId());
        listing.setTitle(request.title().trim());
        listing.setSlug(slugify(request.title()) + "-" + UUID.randomUUID().toString().substring(0, 8));
        listing.setDescription(request.description().trim());
        listing.setListingType(listingType);
        listing.setCondition(condition);
        listing.setStatus("DRAFT");
        listing.setPrice(request.price());
        listing.setNegotiable(request.negotiable());
        listing.setExchangeAllowed(request.exchangeAllowed());
        listing.setProvince(request.province().trim());
        listing.setDistrict(normalizeOptional(request.district()));
        listing.setAddressText(normalizeOptional(request.addressText()));
        listing = listingRepository.save(listing);
        jdbcTemplate.update("""
            insert into marketplace.vehicle_details
              (listing_id, manufacture_year, registration_year, mileage_km, exterior_color, fuel_type,
               transmission, engine_capacity_cc, range_km, seats, owners_count, origin)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, listing.getId(), request.manufactureYear(), request.registrationYear(), request.mileageKm(),
            normalizeOptional(request.exteriorColor()), normalizeEnum(request.fuelType()), normalizeEnum(request.transmission()),
            request.engineCapacityCc(), request.rangeKm(), request.seats(), request.ownersCount(), normalizeOptional(request.origin()));
        return toResponse(listing);
    }

    public List<java.util.Map<String, Object>> categories() {
        return jdbcTemplate.queryForList("select id, name, slug, parent_id as \"parentId\" from marketplace.vehicle_categories where active order by name");
    }

    @Transactional
    public Map<String, Object> toggleFavorite(String username, UUID publicId) {
        VehicleListing listing = listingRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        Integer count = jdbcTemplate.queryForObject("select count(*) from marketplace.listing_favorites where user_id=? and listing_id=?", Integer.class, user.getId(), listing.getId());
        boolean saved;
        if (count != null && count > 0) {
            jdbcTemplate.update("delete from marketplace.listing_favorites where user_id=? and listing_id=?", user.getId(), listing.getId());
            jdbcTemplate.update("update marketplace.vehicle_listings set favorite_count=greatest(favorite_count-1, 0) where id=?", listing.getId());
            saved = false;
        } else {
            jdbcTemplate.update("insert into marketplace.listing_favorites(user_id, listing_id) values (?, ?)", user.getId(), listing.getId());
            jdbcTemplate.update("update marketplace.vehicle_listings set favorite_count=favorite_count+1 where id=?", listing.getId());
            saved = true;
        }
        return Map.of("saved", saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> favoriteStatus(String username, UUID publicId) {
        VehicleListing listing = listingRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        Boolean favorite = jdbcTemplate.queryForObject("select exists(select 1 from marketplace.listing_favorites where user_id=? and listing_id=?)", Boolean.class, user.getId(), listing.getId());
        return Map.of("favorite", Boolean.TRUE.equals(favorite));
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> favoriteListings(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        List<Long> ids = jdbcTemplate.queryForList("select listing_id from marketplace.listing_favorites where user_id=? order by created_at desc", Long.class, user.getId());
        return ids.stream().map(listingRepository::findById).flatMap(java.util.Optional::stream).map(this::toResponse).toList();
    }

    @Transactional
    public Map<String, Object> report(String username, UUID publicId, Map<String, String> body) {
        VehicleListing listing = listingRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        User reporter = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        String reason = normalizeOptional(body.get("reason"));
        if (reason == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report reason is required");
        if (reason.length() > 2000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lý do tối đa 2000 ký tự");
        if (listing.getSeller().getId().equals(reporter.getId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không báo cáo tin của chính mình");
        jdbcTemplate.update("insert into marketplace.listing_reports(listing_id, reporter_id, reason) values (?, ?, ?) on conflict (listing_id, reporter_id) where status='OPEN' do nothing", listing.getId(), reporter.getId(), reason);
        return Map.of("reported", true);
    }

    @Transactional
    public Map<String, Object> uploadImage(String username, UUID publicId, MultipartFile file) {
        VehicleListing listing = listingRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (!listing.getSeller().getUsername().equals(username))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the listing owner can upload images");
        if (!java.util.Set.of("DRAFT","REJECTED").contains(listing.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ thêm ảnh khi tin là bản nháp hoặc bị trả lại. Hãy sửa tin trước.");
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is required");
        if (file.getSize() > maxImageBytes) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image exceeds 10 MB");
        String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = switch (mime) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            default -> throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG, WebP, MP4 and WebM are supported");
        };
        try {
            byte[] bytes = file.getBytes();
            String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            String objectKey = publicId + "-" + UUID.randomUUID() + extension;
            Files.createDirectories(storageRoot);
            Path target = storageRoot.resolve(objectKey).normalize();
            if (!target.startsWith(storageRoot)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image path");
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            Boolean hasPrimary = jdbcTemplate.queryForObject("select exists(select 1 from marketplace.vehicle_images where listing_id=? and primary_image)", Boolean.class, listing.getId());
            String publicUrl = "/api/v1/media/marketplace/" + objectKey;
            Long imageId = jdbcTemplate.queryForObject("""
                insert into marketplace.vehicle_images(listing_id, object_key, public_url, storage_provider, bucket, mime_type, file_size, checksum_sha256, sort_order, primary_image, moderation_status)
                values (?, ?, ?, 'LOCAL', 'local-marketplace', ?, ?, ?,
                        (select count(*) from marketplace.vehicle_images where listing_id=?), ?, 'APPROVED') returning id
                """, Long.class, listing.getId(), objectKey, publicUrl, mime, bytes.length, checksum, listing.getId(), !Boolean.TRUE.equals(hasPrimary));
            return Map.of("id", imageId, "url", publicUrl, "mimeType", mime, "size", bytes.length);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store image", ex);
        }
    }

    @Transactional
    public ListingResponse publish(String username, UUID publicId) {
        VehicleListing listing = listingRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (!listing.getSeller().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the listing owner can publish this listing");
        }
        if (!"DRAFT".equals(listing.getStatus()) && !"REJECTED".equals(listing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing is not ready to publish");
        }
        Integer imageCount = jdbcTemplate.queryForObject(
            "select count(*) from marketplace.vehicle_images where listing_id=? and moderation_status='APPROVED'",
            Integer.class,
            listing.getId()
        );
        if (imageCount == null || imageCount == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one approved image is required");
        }
        listing.setStatus("PENDING_REVIEW");
        return toResponse(listingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public ListingResponse visibleDetail(UUID id, org.springframework.security.core.Authentication auth) {
        VehicleListing listing = listingRepository.findByPublicId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean owner = auth != null && listing.getSeller().getUsername().equals(auth.getName());
        boolean staff = auth != null && auth.getAuthorities().stream().anyMatch(a -> java.util.Set.of("ROLE_ADMIN","ROLE_MANAGER").contains(a.getAuthority()));
        if (!java.util.Set.of("PUBLISHED","RESERVED","SOLD").contains(listing.getStatus()) && !owner && !staff)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tin chưa được công khai");
        return toResponse(listing);
    }

    @Transactional
    public ListingResponse edit(String username, UUID id, CreateListingRequest request) {
        VehicleListing listing = listingRepository.findByPublicId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!listing.getSeller().getUsername().equals(username)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (!java.util.Set.of("DRAFT","REJECTED","PUBLISHED","PENDING_REVIEW").contains(listing.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể sửa tin ở trạng thái này");
        if (!LISTING_TYPES.contains(request.listingType()) || !CONDITIONS.contains(request.condition()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Thông tin xe không hợp lệ");
        Integer count=jdbcTemplate.queryForObject("select count(*) from marketplace.vehicle_categories where id=? and active",Integer.class,request.categoryId());
        if(count==null || count==0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Danh mục không hợp lệ");
        listing.setTitle(request.title().trim()); listing.setDescription(request.description().trim());
        listing.setCategoryId(request.categoryId()); listing.setBrandId(request.brandId());
        listing.setPrice(request.price()); listing.setCondition(request.condition()); listing.setListingType(request.listingType());
        listing.setProvince(request.province().trim()); listing.setDistrict(normalizeOptional(request.district()));
        listing.setNegotiable(request.negotiable()); listing.setExchangeAllowed(request.exchangeAllowed());
        listing.setStatus("DRAFT");
        jdbcTemplate.update("""
            update marketplace.vehicle_details set manufacture_year=?,registration_year=?,mileage_km=?,exterior_color=?,fuel_type=?,transmission=?,
            engine_capacity_cc=?,range_km=?,seats=?,owners_count=?,origin=? where listing_id=?
            """,request.manufactureYear(),request.registrationYear(),request.mileageKm(),normalizeOptional(request.exteriorColor()),
            normalizeEnum(request.fuelType()),normalizeEnum(request.transmission()),request.engineCapacityCc(),request.rangeKm(),
            request.seats(),request.ownersCount(),normalizeOptional(request.origin()),listing.getId());
        return toResponse(listingRepository.save(listing));
    }

    @Transactional
    public ListingResponse removeImage(String username, UUID id, String url) {
        VehicleListing listing=listingRepository.findByPublicId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!listing.getSeller().getUsername().equals(username)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(!java.util.Set.of("DRAFT","REJECTED","PUBLISHED","PENDING_REVIEW").contains(listing.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể sửa ảnh ở trạng thái này");
        int removed=jdbcTemplate.update("delete from marketplace.vehicle_images where listing_id=? and public_url=?",listing.getId(),url);
        if(removed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ảnh không thuộc tin này");
        listing.setStatus("DRAFT");
        return toResponse(listingRepository.save(listing));
    }

    @Transactional
    public ListingResponse withdraw(String username, UUID id) {
        VehicleListing listing=listingRepository.findByPublicId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!listing.getSeller().getUsername().equals(username)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(!java.util.Set.of("DRAFT","REJECTED","PUBLISHED","PENDING_REVIEW").contains(listing.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể gỡ tin ở trạng thái này");
        listing.setStatus("ARCHIVED");
        return toResponse(listingRepository.save(listing));
    }

    private ListingResponse toResponse(VehicleListing listing) {
        User seller = listing.getSeller();
        String sellerName = ((seller.getFirstName() == null ? "" : seller.getFirstName()) + " " + (seller.getLastName() == null ? "" : seller.getLastName())).trim();
        if (sellerName.isBlank()) sellerName = seller.getUsername();
        List<String> images = jdbcTemplate.queryForList("select public_url from marketplace.vehicle_images where listing_id=? and moderation_status='APPROVED' order by primary_image desc, sort_order", String.class, listing.getId());
        String imageUrl = images.isEmpty() ? null : images.get(0);
        Map<String, Object> details = jdbcTemplate.query("select manufacture_year, registration_year, mileage_km, exterior_color, fuel_type::text, transmission::text, engine_capacity_cc, range_km, seats, owners_count, origin from marketplace.vehicle_details where listing_id=?", rs -> {
            if (!rs.next()) return Map.of();
            Map<String, Object> row = new java.util.HashMap<>();
            String[] names = {"manufacture_year", "registration_year", "mileage_km", "exterior_color", "fuel_type", "transmission", "engine_capacity_cc", "range_km", "seats", "owners_count", "origin"};
            for (String name : names) row.put(name, rs.getObject(name));
            return row;
        }, listing.getId());
        return new ListingResponse(listing.getId(), listing.getPublicId(), seller.getId(), sellerName, seller.getPhoneNumber(), listing.getCategoryId(), listing.getBrandId(), listing.getTitle(), listing.getSlug(), listing.getDescription(), listing.getListingType(), listing.getCondition(), listing.getStatus(), listing.getPrice(), listing.isNegotiable(), listing.isExchangeAllowed(), listing.getProvince(), listing.getDistrict(), listing.getAddressText(), imageUrl, images,
            integerValue(details.get("manufacture_year")), integerValue(details.get("registration_year")), integerValue(details.get("mileage_km")), (String) details.get("exterior_color"), (String) details.get("fuel_type"), (String) details.get("transmission"), integerValue(details.get("engine_capacity_cc")), integerValue(details.get("range_km")), integerValue(details.get("seats")), integerValue(details.get("owners_count")), (String) details.get("origin"),
            listing.getViewCount(), listing.getFavoriteCount(), listing.getPublishedAt(), listing.getCreatedAt(),
            jdbcTemplate.query("select note from marketplace.listing_moderations where listing_id=? and reason_code is distinct from 'USER_REPORT' order by id desc limit 1", rs -> rs.next() ? rs.getString(1) : null, listing.getId()));
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
    private String normalizeOptional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String normalizeEnum(String value) { return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT); }
    private Integer integerValue(Object value) { return value instanceof Number number ? number.intValue() : null; }
}
