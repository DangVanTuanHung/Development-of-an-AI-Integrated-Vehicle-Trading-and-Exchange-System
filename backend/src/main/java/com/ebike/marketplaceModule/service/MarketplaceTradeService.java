package com.ebike.marketplaceModule.service;

import com.ebike.authModule.entity.User;
import com.ebike.authModule.repository.UserRepository;
import com.ebike.marketplaceModule.dto.CreateOfferRequest;
import com.ebike.marketplaceModule.dto.CreatePaymentRequest;
import java.math.BigDecimal;
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
public class MarketplaceTradeService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public MarketplaceTradeService(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Transactional
    public Map<String, Object> createOffer(String username, UUID listingPublicId, CreateOfferRequest request) {
        User buyer = requireUser(username);
        Map<String, Object> listing = one("select id, seller_id, price, status from marketplace.vehicle_listings where public_id = ?", listingPublicId);
        long listingId = ((Number) listing.get("id")).longValue();
        long sellerId = ((Number) listing.get("seller_id")).longValue();
        if (sellerId == buyer.getId()) throw badRequest("Bạn không thể trả giá cho tin của chính mình");
        if (!"PUBLISHED".equals(listing.get("status"))) throw badRequest("Tin đăng không còn nhận đề nghị");
        if (request.depositAmount().compareTo(request.amount()) > 0) throw badRequest("Tiền cọc không được lớn hơn giá đề nghị");
        UUID publicId = UUID.randomUUID();
        Long id = jdbc.queryForObject("""
            insert into marketplace.offers(public_id, listing_id, buyer_id, seller_id, amount, deposit_amount, message, expires_at)
            values (?, ?, ?, ?, ?, ?, ?, ?) returning id
            """, Long.class, publicId, listingId, buyer.getId(), sellerId, request.amount(), request.depositAmount(), clean(request.message()), OffsetDateTime.now().plusDays(3));
        return offer(id);
    }

    public List<Map<String, Object>> myOffers(String username) {
        User user = requireUser(username);
        return jdbc.queryForList("""
            select o.id, o.public_id as "publicId", o.listing_id as "listingId", l.title as "listingTitle",
                   o.buyer_id as "buyerId", o.seller_id as "sellerId", o.amount, o.deposit_amount as "depositAmount",
                   o.message, o.status, o.expires_at as "expiresAt", o.created_at as "createdAt",
                   coalesce(nullif(trim(b.first_name || ' ' || b.last_name), ''), b.username) as "buyerName",
                   b.email as "buyerEmail",
                   coalesce(b.phone_number, (select s.phone_number from ebike_order.orders bo join ebike_order.shipments s on s.order_id=bo.id
                    where bo.user_id=o.buyer_id and s.phone_number is not null order by bo.created_at desc limit 1)) as "buyerPhone",
                   (select concat_ws(', ', nullif(a.street,''), nullif(a.city,''), nullif(a.country,''))
                    from ebike_user.user_addresses a where a.user_id=o.buyer_id
                    order by a.is_default desc, a.id desc limit 1) as "buyerAddress"
            from marketplace.offers o
            join marketplace.vehicle_listings l on l.id=o.listing_id
            join ebike_auth.users b on b.id=o.buyer_id
            where o.buyer_id=? or o.seller_id=? order by o.created_at desc
            """, user.getId(), user.getId());
    }

    @Transactional
    public Map<String, Object> acceptOffer(String username, UUID offerPublicId) {
        User seller = requireUser(username);
        Map<String, Object> offer = one("select * from marketplace.offers where public_id=? for update", offerPublicId);
        if (((Number) offer.get("seller_id")).longValue() != seller.getId()) throw forbidden();
        if (!"PENDING".equals(offer.get("status")) && !"COUNTERED".equals(offer.get("status"))) throw badRequest("Đề nghị không còn hiệu lực");
        Long listingId = ((Number) offer.get("listing_id")).longValue();
        Map<String,Object> currentListing=one("select status from marketplace.vehicle_listings where id=? for update",listingId);
        if(!"PUBLISHED".equals(currentListing.get("status"))) throw badRequest("Tin không còn nhận giao dịch");
        Boolean expired=jdbc.queryForObject("select expires_at < now() from marketplace.offers where id=?",Boolean.class,offer.get("id"));
        if(Boolean.TRUE.equals(expired)) throw badRequest("Đề nghị đã hết hạn");
        Integer active = jdbc.queryForObject("select count(*) from marketplace.transactions where listing_id=? and status not in ('COMPLETED','CANCELLED','REFUNDED')", Integer.class, listingId);
        if (active != null && active > 0) throw badRequest("Tin đăng đã có giao dịch đang xử lý");
        jdbc.update("update marketplace.offers set status='ACCEPTED', responded_at=now() where id=?", offer.get("id"));
        jdbc.update("update marketplace.offers set status='REJECTED', responded_at=now() where listing_id=? and id<>? and status in ('PENDING','COUNTERED')", listingId, offer.get("id"));
        jdbc.update("update marketplace.vehicle_listings set status='RESERVED', updated_at=now() where id=?", listingId);
        UUID publicId = UUID.randomUUID();
        String number = "TX-" + OffsetDateTime.now().toLocalDate().toString().replace("-", "") + "-" + publicId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
        Long transactionId = jdbc.queryForObject("""
            insert into marketplace.transactions(public_id, transaction_number, listing_id, accepted_offer_id, buyer_id, seller_id, agreed_price, deposit_amount, status, inspection_deadline)
            values (?, ?, ?, ?, ?, ?, ?, ?, 'DEPOSIT_PENDING', ?) returning id
            """, Long.class, publicId, number, listingId, offer.get("id"), offer.get("buyer_id"), seller.getId(), offer.get("amount"), offer.get("deposit_amount"), OffsetDateTime.now().plusDays(7));
        jdbc.update("insert into marketplace.transaction_events(transaction_id, actor_id, from_status, to_status, event_type, note) values (?, ?, 'CREATED', 'DEPOSIT_PENDING', 'OFFER_ACCEPTED', 'Người bán đã chấp nhận đề nghị')", transactionId, seller.getId());
        return transaction(transactionId);
    }

    public List<Map<String, Object>> myTransactions(String username) {
        User user = requireUser(username);
        return jdbc.queryForList("""
            select t.id, t.public_id as "publicId", t.transaction_number as "transactionNumber", t.listing_id as "listingId",
                   l.title as "listingTitle", t.buyer_id as "buyerId", t.seller_id as "sellerId", t.agreed_price as "agreedPrice",
                   t.deposit_amount as "depositAmount", t.remaining_amount as "remainingAmount", t.status, t.created_at as "createdAt",
                   p.public_id as "paymentPublicId", p.payment_stage as "paymentStage", p.provider as "paymentProvider",
                   p.amount as "paymentAmount", p.status as "paymentStatus", p.payment_reference as "paymentReference"
            from marketplace.transactions t join marketplace.vehicle_listings l on l.id=t.listing_id
            left join lateral (select * from marketplace_payment.payments mp where mp.transaction_id=t.id order by mp.created_at desc limit 1) p on true
            where t.buyer_id=? or t.seller_id=? order by t.created_at desc
            """, user.getId(), user.getId());
    }

    @Transactional
    public Map<String, Object> createPayment(String username, UUID transactionPublicId, CreatePaymentRequest request) {
        User user = requireUser(username);
        Map<String, Object> tx = one("select * from marketplace.transactions where public_id=? for update", transactionPublicId);
        if (((Number) tx.get("buyer_id")).longValue() != user.getId()) throw forbidden();
        String stage = request.stage().trim().toUpperCase(Locale.ROOT);
        String provider = request.provider().trim().toUpperCase(Locale.ROOT);
        if (!List.of("DEPOSIT", "FINAL").contains(stage)) throw badRequest("Giai đoạn thanh toán không hợp lệ");
        if (!List.of("VNPAY", "BANK_TRANSFER", "CASH_ON_DELIVERY").contains(provider)) throw badRequest("Nhà cung cấp thanh toán không hợp lệ");
        boolean cashOnDelivery = "CASH_ON_DELIVERY".equals(provider);
        if (cashOnDelivery && !"DEPOSIT_PENDING".equals(tx.get("status"))) throw badRequest("Chỉ chọn tiền mặt khi giao dịch mới chờ đặt cọc");
        if (cashOnDelivery && !"FINAL".equals(stage)) throw badRequest("Tiền mặt khi nhận xe phải là thanh toán toàn bộ");
        if (!cashOnDelivery && "DEPOSIT".equals(stage) && !"DEPOSIT_PENDING".equals(tx.get("status"))) throw badRequest("Giao dịch không chờ đặt cọc");
        if (!cashOnDelivery && "FINAL".equals(stage) && !List.of("DEPOSIT_PAID", "INSPECTION_PENDING", "FINAL_PAYMENT_PENDING").contains(tx.get("status"))) throw badRequest("Giao dịch chưa sẵn sàng thanh toán phần còn lại");
        BigDecimal amount = (BigDecimal) (cashOnDelivery ? tx.get("agreed_price") : "DEPOSIT".equals(stage) ? tx.get("deposit_amount") : tx.get("remaining_amount"));
        if (amount.signum() <= 0) throw badRequest("Số tiền thanh toán phải lớn hơn 0");
        UUID publicId = UUID.randomUUID();
        String reference = "PAY-" + publicId.toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        String idempotency = tx.get("id") + ":" + stage + ":" + provider;
        Long id = jdbc.queryForObject("""
            insert into marketplace_payment.payments(public_id, transaction_id, payment_reference, payment_stage, provider, amount, status, idempotency_key, expires_at)
            values (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?) returning id
            """, Long.class, publicId, tx.get("id"), reference, stage, provider, amount, idempotency, OffsetDateTime.now().plusMinutes(30));
        if (cashOnDelivery) {
            jdbc.update("update marketplace.transactions set status='INSPECTION_PENDING', updated_at=now() where id=?", tx.get("id"));
            jdbc.update("insert into marketplace.transaction_events(transaction_id,actor_id,from_status,to_status,event_type,note) values (?,?,'DEPOSIT_PENDING','INSPECTION_PENDING','CASH_ON_DELIVERY_SELECTED','Người mua chọn trả tiền mặt khi nhận xe')", tx.get("id"), user.getId());
        }
        return one("select id, public_id as \"publicId\", payment_reference as \"paymentReference\", payment_stage as \"stage\", provider, amount, currency, status, expires_at as \"expiresAt\" from marketplace_payment.payments where id=?", id);
    }

    @Transactional
    public Map<String, Object> confirmPayment(String username, UUID paymentPublicId) {
        User seller = requireUser(username);
        Map<String, Object> payment = one("""
            select p.*, t.seller_id, t.listing_id, t.status as transaction_status
            from marketplace_payment.payments p join marketplace.transactions t on t.id=p.transaction_id
            where p.public_id=? for update
            """, paymentPublicId);
        if (((Number) payment.get("seller_id")).longValue() != seller.getId()) throw forbidden();
        if (!"PENDING".equals(payment.get("status")) && !"PROCESSING".equals(payment.get("status"))) throw badRequest("Khoản thanh toán không còn chờ xác nhận");
        if (java.util.Set.of("DISPUTED","CANCELLED","REFUNDED","COMPLETED").contains(String.valueOf(payment.get("transaction_status"))))
            throw badRequest("Giao dịch đang bị chặn hoặc đã kết thúc");
        if ("VNPAY".equals(payment.get("provider"))) throw badRequest("Thanh toán VNPAY phải được xác nhận qua cổng thanh toán");
        jdbc.update("update marketplace_payment.payments set status='PAID', paid_at=now(), updated_at=now() where id=?", payment.get("id"));
        String stage = String.valueOf(payment.get("payment_stage"));
        String provider = String.valueOf(payment.get("provider"));
        String fromStatus = String.valueOf(payment.get("transaction_status"));
        boolean cashOnDelivery = "CASH_ON_DELIVERY".equals(provider);
        String nextStatus = "DEPOSIT".equals(stage) ? "DEPOSIT_PAID" : cashOnDelivery ? "COMPLETED" : "FULLY_PAID";
        jdbc.update("update marketplace.transactions set status=?, completed_at=case when ?='COMPLETED' then now() else completed_at end, updated_at=now() where id=?", nextStatus, nextStatus, payment.get("transaction_id"));
        if ("FULLY_PAID".equals(nextStatus)) {
            Integer payoutCount = jdbc.queryForObject("select count(*) from marketplace_payment.payouts where transaction_id=?", Integer.class, payment.get("transaction_id"));
            if (payoutCount == null || payoutCount == 0) jdbc.update("insert into marketplace_payment.payouts(transaction_id,seller_id,gross_amount,fee_amount,net_amount,status,bank_account_snapshot) select id,seller_id,agreed_price,0,agreed_price,'ON_HOLD','{}'::jsonb from marketplace.transactions where id=?", payment.get("transaction_id"));
        }
        if ("COMPLETED".equals(nextStatus)) jdbc.update("update marketplace.vehicle_listings set status='SOLD', updated_at=now() where id=?", payment.get("listing_id"));
        jdbc.update("insert into marketplace.transaction_events(transaction_id,actor_id,from_status,to_status,event_type,note) values (?,?,?,?,?,?)",
            payment.get("transaction_id"), seller.getId(), fromStatus, nextStatus, "PAYMENT_CONFIRMED", "CASH_ON_DELIVERY".equals(provider) ? "Người bán xác nhận đã nhận tiền mặt và bàn giao xe" : "Người bán xác nhận đã nhận chuyển khoản");
        return one("select public_id as \"publicId\", payment_reference as \"paymentReference\", payment_stage as stage, provider, amount, status, paid_at as \"paidAt\" from marketplace_payment.payments where id=?", payment.get("id"));
    }

    @Transactional
    public Map<String, Object> confirmHandover(String username, UUID transactionPublicId) {
        User buyer = requireUser(username);
        Map<String, Object> tx = one("select * from marketplace.transactions where public_id=? for update", transactionPublicId);
        if (((Number) tx.get("buyer_id")).longValue() != buyer.getId()) throw forbidden();
        if (!"FULLY_PAID".equals(tx.get("status"))) throw badRequest("Giao dịch chưa thanh toán đủ hoặc đang có tranh chấp");
        jdbc.update("update marketplace.transactions set status='COMPLETED', completed_at=now(), updated_at=now() where id=?", tx.get("id"));
        jdbc.update("update marketplace.vehicle_listings set status='SOLD', updated_at=now() where id=?", tx.get("listing_id"));
        jdbc.update("update marketplace_payment.payouts set status='READY', released_at=now() where transaction_id=? and status='ON_HOLD'", tx.get("id"));
        jdbc.update("insert into marketplace.transaction_events(transaction_id,actor_id,from_status,to_status,event_type,note) values (?,?,'FULLY_PAID','COMPLETED','BUYER_CONFIRMED_HANDOVER','Người mua xác nhận đã nhận xe; khoản ký quỹ sẵn sàng chi trả cho người bán')", tx.get("id"), buyer.getId());
        return transaction(((Number) tx.get("id")).longValue());
    }

    private Map<String, Object> offer(Long id) { return one("select id, public_id as \"publicId\", listing_id as \"listingId\", buyer_id as \"buyerId\", seller_id as \"sellerId\", amount, deposit_amount as \"depositAmount\", message, status, expires_at as \"expiresAt\", created_at as \"createdAt\" from marketplace.offers where id=?", id); }
    private Map<String, Object> transaction(Long id) { return one("select id, public_id as \"publicId\", transaction_number as \"transactionNumber\", listing_id as \"listingId\", buyer_id as \"buyerId\", seller_id as \"sellerId\", agreed_price as \"agreedPrice\", deposit_amount as \"depositAmount\", remaining_amount as \"remainingAmount\", status, created_at as \"createdAt\" from marketplace.transactions where id=?", id); }
    private Map<String, Object> one(String sql, Object... args) { List<Map<String, Object>> rows = jdbc.queryForList(sql, args); if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"); return rows.get(0); }
    private User requireUser(String username) { return users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)); }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException forbidden() { return new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
