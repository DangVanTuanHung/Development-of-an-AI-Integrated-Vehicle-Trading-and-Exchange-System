package com.ebike.marketplaceModule.advisor;

import java.util.regex.Pattern;

/** Scope checks run before conversation memory or catalogue access. */
public final class AdvisorScope {
    private AdvisorScope() {}
    private static boolean contains(String text, String words) {
        return Pattern.compile("\\b(?:" + words + ")\\b").matcher(text).find();
    }
    public static boolean unrelated(String text) {
        return contains(text, "ronaldo|messi|bong da|bong ro|cau thu|the thao|world cup|ca si|bai hat|nau an|cong thuc nau|thoi tiet|tu vi|chinh tri|tong thong|giai toan|viet code|lap trinh");
    }
    public static boolean vehicleTopic(String text) {
        return contains(text, "xe|oto|o to|phuong tien|toyota|honda|vinfast|kia|ford|mazda|hyundai|vios|accent|vision|vespa|suzuki|mitsubishi|nissan|pega|yadea|suv|sedan|hatchback|pickup|dong co|phanh|lop xe|bao duong|pin|sac|nhien lieu|hop so|odo|marketplace|motionx");
    }
    public static boolean ambiguousComparison(String text) {
        return contains(text, "so sanh|cai nao ngon hon|nen mua|nen chon|dinh gia");
    }
    public static boolean comparisonReference(String text) {
        return text.matches("(?:so sanh )?(?:cac )?(?:xe|mau|chiec|con|cai)(?: nao| nay| do| vua roi| tot hon| ngon hon)*[.,]*");
    }
    public static boolean followUp(String text) {
        return text.matches("(?:co )?(?:mau nao )?re hon(?: nua| khong)?[.,]*")
            || text.matches("(?:cho toi |cho minh )?(?:xem |co )?(?:hinh anh|hinh|anh)(?: khong)?[.,]*")
            || text.matches("(?:xe |chiec |con |mau )?(?:thu (?:1|2|hai)|dau tien|do|nay)[.,]*")
            || text.matches("(?:doi cao hon|ok|okay|duoc|dong y|tiep tuc|roi sao|huong dan di|ban huong dan di|ok ban huong dan di)[.,]*")
            || text.matches("(?:tam |khoang |duoi |toi da |ngan sach )?\\d+(?:[.,]\\d+)?\\s*(?:ty|trieu|tr)(?: thoi| mua (?:con )?gi)?[.,]*");
    }
}
