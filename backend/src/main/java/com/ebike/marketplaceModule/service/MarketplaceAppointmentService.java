package com.ebike.marketplaceModule.service;

import com.ebike.authModule.entity.User;
import com.ebike.authModule.repository.UserRepository;
import com.ebike.marketplaceModule.dto.CreateAppointmentRequest;
import com.ebike.marketplaceModule.dto.UpdateAppointmentRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MarketplaceAppointmentService {
    private static final List<String> STATUSES = List.of("CONFIRMED", "DECLINED", "COMPLETED", "CANCELLED");

    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public MarketplaceAppointmentService(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Transactional
    public Map<String, Object> create(String username, UUID listingPublicId, CreateAppointmentRequest request) {
        User buyer = requireUser(username);
        Map<String, Object> listing = one("""
            select id, seller_id, status
            from marketplace.vehicle_listings
            where public_id = ?
            """, listingPublicId);
        long sellerId = ((Number) listing.get("seller_id")).longValue();
        if (sellerId == buyer.getId()) throw badRequest("Bạn không thể đặt lịch cho tin của mình");
        if (!"PUBLISHED".equals(listing.get("status"))) throw badRequest("Tin đăng không còn nhận lịch xem");
        if (request.scheduledAt().isBefore(OffsetDateTime.now())) throw badRequest("Thời gian xem xe phải ở tương lai");
        UUID publicId = UUID.randomUUID();
        Long id = jdbc.queryForObject("""
            insert into marketplace.appointments(public_id, listing_id, buyer_id, seller_id, scheduled_at, location, note)
            values (?, ?, ?, ?, ?, ?, ?)
            returning id
            """, Long.class, publicId, listing.get("id"), buyer.getId(), sellerId,
            request.scheduledAt(), request.location().trim(), clean(request.note()));
        return appointment(id);
    }

    public List<Map<String, Object>> mine(String username) {
        User user = requireUser(username);
        return jdbc.queryForList(query() + " where a.buyer_id = ? or a.seller_id = ? order by a.scheduled_at asc", user.getId(), user.getId());
    }

    @Transactional
    public Map<String, Object> update(String username, UUID appointmentPublicId, UpdateAppointmentRequest request) {
        User user = requireUser(username);
        String status = request.status().trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) throw badRequest("Trạng thái lịch hẹn không hợp lệ");
        Map<String, Object> appointment = one("select * from marketplace.appointments where public_id = ? for update", appointmentPublicId);
        boolean buyer = ((Number) appointment.get("buyer_id")).longValue() == user.getId();
        boolean seller = ((Number) appointment.get("seller_id")).longValue() == user.getId();
        if (!buyer && !seller) throw forbidden();
        String current = String.valueOf(appointment.get("status"));
        if ("CONFIRMED".equals(status) || "DECLINED".equals(status)) {
            if (!seller || !"REQUESTED".equals(current)) throw badRequest("Chỉ người bán mới có thể phản hồi lịch đang chờ");
        } else if ("COMPLETED".equals(status)) {
            if (!buyer && !seller || !"CONFIRMED".equals(current)) throw badRequest("Lịch phải được xác nhận trước khi hoàn thành");
        } else if ("CANCELLED".equals(status)) {
            if (!List.of("REQUESTED", "CONFIRMED").contains(current)) throw badRequest("Lịch hẹn không còn có thể hủy");
        }
        jdbc.update("update marketplace.appointments set status=?, seller_note=?, responded_at=case when ? in ('CONFIRMED','DECLINED') then now() else responded_at end, updated_at=now() where id=?", status, clean(request.note()), status, appointment.get("id"));
        return appointment(((Number) appointment.get("id")).longValue());
    }

    private String query() {
        return """
            select a.public_id as \"publicId\", a.listing_id as \"listingId\", l.title as \"listingTitle\",
                   a.buyer_id as \"buyerId\", a.seller_id as \"sellerId\", a.scheduled_at as \"scheduledAt\",
                   a.location, a.note, a.seller_note as \"sellerNote\", a.status,
                   a.created_at as \"createdAt\", a.responded_at as \"respondedAt\",
                   coalesce(nullif(trim(b.first_name || ' ' || b.last_name), ''), b.username) as \"buyerName\",
                   coalesce(nullif(trim(s.first_name || ' ' || s.last_name), ''), s.username) as \"sellerName\"
            from marketplace.appointments a
            join marketplace.vehicle_listings l on l.id = a.listing_id
            join ebike_auth.users b on b.id = a.buyer_id
            join ebike_auth.users s on s.id = a.seller_id
            """;
    }

    private Map<String, Object> appointment(Long id) {
        return jdbc.queryForList(query() + " where a.id = ?", id).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        return rows.get(0);
    }

    private User requireUser(String username) {
        return users.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác lịch này");
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
