package com.ebike.marketplaceModule.service;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class OperationsPolicy {
    private OperationsPolicy() {}
    public static final Set<String> REASONS = Set.of("DUPLICATE_LISTING", "WRONG_CATEGORY", "UNREALISTIC_PRICE", "INVALID_IMAGE", "MISSING_INFORMATION", "SUSPICIOUS_CONTENT", "INVALID_DOCUMENT", "OTHER");
    public static String transition(String status, String action, boolean own, String reason, String note) {
        if (own) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không được kiểm duyệt tin của chính mình");
        String next = switch (action) {
            case "APPROVE" -> "PENDING_REVIEW".equals(status) ? "PUBLISHED" : null;
            case "REJECT", "REQUEST_EDIT" -> "PENDING_REVIEW".equals(status) ? "REJECTED" : null;
            case "HIDE" -> "PUBLISHED".equals(status) ? "SUSPENDED" : null;
            case "RESTORE" -> "SUSPENDED".equals(status) ? "PENDING_REVIEW" : null;
            default -> null;
        };
        if (next == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Thao tác không phù hợp với trạng thái hiện tại. Hãy tải lại.");
        if (!"APPROVE".equals(action) && (!REASONS.contains(reason == null ? "" : reason) || note == null || note.isBlank() || note.length() > 2000))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn lý do và nhập ghi chú (tối đa 2000 ký tự)");
        return next;
    }
}
