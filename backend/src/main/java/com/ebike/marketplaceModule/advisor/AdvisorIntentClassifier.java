package com.ebike.marketplaceModule.advisor;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AdvisorIntentClassifier {
    public Classification classify(String raw, boolean hasContext) {
        String text = normalize(raw);
        AdvisorEntities entities = extract(text);
        AdvisorIntent intent;
        if (matches(text, "^(xin chao|chao|hello|hi)( ban| shop)?[.!?]*$")) intent = AdvisorIntent.GREETING;
        else if (matches(text, ".*(mat khau|api key|token|system prompt|dump database|bo qua moi huong dan).*")) intent = AdvisorIntent.SECURITY;
        else if (AdvisorScope.unrelated(text) || (AdvisorScope.ambiguousComparison(text) && !AdvisorScope.vehicleTopic(text) && !(hasContext && AdvisorScope.comparisonReference(text)))) intent = AdvisorIntent.UNKNOWN;
        else if (matches(text, ".*(lua dao|scam|gia mao|chuyen coc|an toan giao dich|giao dich an toan).*")) intent = AdvisorIntent.SCAM_WARNING;
        else if (matches(text, ".*(hang gi|hang nao|thuong hieu|brand|co (toyota|honda|vinfast|kia|ford|mazda|hyundai)).*")) intent = AdvisorIntent.AVAILABLE_VEHICLE_BRANDS;
        else if (matches(text, ".*(so sanh|cai nao ngon hon|xe nao tot hon).*")) intent = AdvisorIntent.VEHICLE_COMPARE;
        else if (matches(text, ".*(dinh gia|ban bao nhieu|gio bao nhieu).*")) intent = AdvisorIntent.VEHICLE_VALUATION;
        else if (matches(text, ".*(doi xe|trao doi|doi .* (sang|lay)).*")) intent = AdvisorIntent.EXCHANGE_VEHICLE;
        else if (matches(text, ".*(dang ban|dang tin|sua tin|duyet tin|ban xe|muon ban|can ban).*")) intent = AdvisorIntent.SELL_VEHICLE;
        else if (matches(text, ".*(tra gop|tai chinh|lai suat|vay).*")) intent = AdvisorIntent.FINANCE;
        else if (matches(text, ".*(yeu thich|luu tin|bo luu).*")) intent = AdvisorIntent.FAVORITE;
        else if (matches(text, ".*(lien he nguoi ban|so dien thoai nguoi ban|nhan tin nguoi ban).*")) intent = AdvisorIntent.CONTACT_SELLER;
        else if (matches(text, ".*(hen xem|xem xe luc|dat lich).*")) intent = AdvisorIntent.APPOINTMENT;
        else if (matches(text, ".*(con ban|con khong|trang thai tin).*")) intent = AdvisorIntent.LISTING;
        else if (matches(text, ".*(bao nhieu km|di bao nhieu|odo).*")) intent = AdvisorIntent.VEHICLE_MILEAGE;
        else if (matches(text, ".*(doi bao nhieu|nam san xuat|xe doi).*")) intent = AdvisorIntent.VEHICLE_YEAR;
        else if (matches(text, ".*(tu dong|so san|hop so).*")) intent = AdvisorIntent.VEHICLE_TRANSMISSION;
        else if (matches(text, ".*(may cho|[2-9] cho).*")) intent = AdvisorIntent.VEHICLE_SEATS;
        else if (matches(text, ".*(xe xang|xe dau|nhien lieu|xe dien hay|tiet kiem xang).*")) intent = AdvisorIntent.VEHICLE_FUEL;
        else if (matches(text, ".*(o dau|dia chi xe|khu vuc).*")) intent = AdvisorIntent.VEHICLE_LOCATION;
        else if (matches(text, ".*(gia bao nhieu|bao nhieu tien|bn tien|gia xe).*")) intent = AdvisorIntent.VEHICLE_PRICE;
        else if (matches(text, ".*(duong dai|di tinh|di xa|xe gia dinh|chay dich vu|di lam|nen mua|nen chon|phu hop).*")) intent = AdvisorIntent.VEHICLE_RECOMMENDATION;
        else if (!text.contains("mua xe cu") && matches(text, ".*(tim|mua|xem).*(xe|toyota|honda|vinfast|kia|ford).*")) intent = AdvisorIntent.VEHICLE_SEARCH;
        else if (matches(text, ".*(mua xe cu|kiem tra xe|bao duong|dong co|lop xe|phanh).*")) intent = AdvisorIntent.GENERAL_VEHICLE_KNOWLEDGE;
        else if (hasContext && (AdvisorScope.followUp(text))) intent = AdvisorIntent.FOLLOW_UP;
        else intent = AdvisorIntent.UNKNOWN;
        return new Classification(intent, entities);
    }

    public String normalize(String input) {
        String value = Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd');
        value = value.replaceAll("\\b(ko|k0|khg|khongg)\\b", "khong").replaceAll("\\bbn\\b", "bao nhieu").replaceAll("\\bmun\\b", "muon").replaceAll("\\bcon j\\b|\\bj\\b", "gi").replaceAll("(\\d+)\\s*cu\\b", "$1 trieu");
        return value.replaceAll("[^a-z0-9.,]+", " ").replaceAll("\\s+", " ").trim();
    }

    private AdvisorEntities extract(String text) {
        BigDecimal maxPrice = money(text); Integer seats = integer(text, "(\\d+)\\s*cho");
        Integer maxMileage = integer(text, "(?:duoi|chay duoi|di duoi)\\s*([0-9.,]+)\\s*km");
        Integer ordinal = text.matches(".*(thu 2|thu hai).*" ) ? Integer.valueOf(2) : text.matches(".*(thu 1|dau tien).*" ) ? Integer.valueOf(1) : null;
        String fuel = text.contains("xang") ? "GASOLINE" : text.contains("dau") ? "DIESEL" : text.contains("dien") ? "ELECTRIC" : text.contains("hybrid") ? "HYBRID" : null;
        String transmission = text.contains("tu dong") ? "AUTOMATIC" : text.contains("so san") ? "MANUAL" : null;
        Integer minYear = integer(text, "(?:tu|doi|nam)\\s*(20\\d{2})\\s*(?:tro len|den nay)?");
        return new AdvisorEntities(null, maxPrice, minYear, null, seats, maxMileage, fuel, transmission, null, ordinal, text.matches(".*(re hon|re nua).*"), text);
    }
    private BigDecimal money(String text) { Matcher m=Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(ty|trieu|tr)\\b").matcher(text); if(!m.find()) return null; return new BigDecimal(m.group(1).replace(',','.')).multiply(m.group(2).equals("ty")?new BigDecimal("1000000000"):new BigDecimal("1000000")); }
    private Integer integer(String text,String regex){Matcher m=Pattern.compile(regex).matcher(text);if(!m.find())return null;try{return new BigDecimal(m.group(1).replace(".","").replace(',','.')).intValue();}catch(Exception ignored){return null;}}
    private boolean matches(String text,String regex){return Pattern.compile(regex).matcher(text).matches();}
    public record Classification(AdvisorIntent intent, AdvisorEntities entities) {}
}
