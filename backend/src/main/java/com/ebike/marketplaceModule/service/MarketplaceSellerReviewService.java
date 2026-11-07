package com.ebike.marketplaceModule.service;

import com.ebike.authModule.entity.User;
import com.ebike.authModule.repository.UserRepository;
import com.ebike.marketplaceModule.dto.CreateSellerReviewRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MarketplaceSellerReviewService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public MarketplaceSellerReviewService(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Transactional
    public Map<String, Object> createReview(String username, UUID transactionPublicId, CreateSellerReviewRequest request) {
        User reviewer = requireUser(username);
        Map<String, Object> transaction = one("""
            select id, buyer_id, seller_id, status
            from marketplace.transactions
            where public_id = ?
            for update
            """, transactionPublicId);
        long buyerId=((Number)transaction.get("buyer_id")).longValue();
        long sellerId=((Number)transaction.get("seller_id")).longValue();
        if (buyerId != reviewer.getId() && sellerId != reviewer.getId()) {
            throw forbidden();
        }
        if (!"COMPLETED".equals(transaction.get("status"))) {
            throw badRequest("Chỉ có thể đánh giá sau khi giao dịch hoàn tất");
        }
        Integer existing = jdbc.queryForObject(
            "select count(*) from marketplace.seller_reviews where transaction_id = ? and reviewer_id=?",
            Integer.class,
            transaction.get("id"),reviewer.getId()
        );
        if (existing != null && existing > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch này đã được đánh giá");
        }
        String comment = clean(request.comment());
        Long id = jdbc.queryForObject("""
            insert into marketplace.seller_reviews(transaction_id, reviewer_id, seller_id, rating, comment)
            values (?, ?, ?, ?, ?)
            returning id
            """, Long.class, transaction.get("id"), reviewer.getId(), buyerId==reviewer.getId()?sellerId:buyerId, request.rating(), comment);
        return review(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reputation(Long sellerId) {
        Map<String, Object> seller = one("""
            select id, username, first_name as "firstName", last_name as "lastName", created_at as "createdAt",
                   avatar_url as "avatarUrl", is_verified as verified,
                   email_verified_at as "emailVerifiedAt", phone_verified_at as "phoneVerifiedAt"
            from ebike_auth.users
            where id = ? and is_active = true
            """, sellerId);
        Map<String, Object> summary = one("""
            select coalesce(round(avg(rating)::numeric, 2), 0) as "averageRating",
                   count(*) as "reviewCount",
                   (select count(*) from marketplace.vehicle_listings where seller_id=? and status='PUBLISHED') as "activeListingCount",
                   (select count(*) from marketplace.transactions where (seller_id=? or (buyer_id=? and exchange_listing_id is not null)) and status='COMPLETED') as "completedVehicleCount"
            from marketplace.seller_reviews
            where seller_id = ? and status='VISIBLE'
            """, sellerId, sellerId, sellerId, sellerId);
        List<Map<String, Object>> reviews = jdbc.queryForList("""
            select r.public_id as "publicId", r.rating, r.comment,r.reply,r.replied_at as "repliedAt",r.reviewer_id as "reviewerId",r.seller_id as "targetId",
                   r.created_at as "createdAt", u.username as "reviewerUsername",
                   coalesce(nullif(trim(u.first_name || ' ' || u.last_name), ''), u.username) as "reviewerName"
            from marketplace.seller_reviews r
            join ebike_auth.users u on u.id = r.reviewer_id
            where r.seller_id = ? and r.status='VISIBLE'
            order by r.created_at desc
            limit 50
            """, sellerId);
        return Map.of("seller", seller, "summary", summary, "reviews", reviews);
    }

    private Map<String, Object> review(Long id) {
        return one("""
            select r.public_id as "publicId", r.rating, r.comment,
                   r.created_at as "createdAt", r.transaction_id as "transactionId"
            from marketplace.seller_reviews r
            where r.id = ?
            """, id);
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
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền đánh giá giao dịch này");
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
