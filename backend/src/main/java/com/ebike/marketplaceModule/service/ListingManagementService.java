package com.ebike.marketplaceModule.service;

import com.ebike.marketplaceModule.dto.ListingResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ListingManagementService {
    private final JdbcTemplate jdbc;
    private final MarketplaceListingService listings;
    private final String hashSecret;
    public ListingManagementService(JdbcTemplate jdbc, MarketplaceListingService listings, @Value("${jwt.secret}") String hashSecret) {
        this.jdbc=jdbc; this.listings=listings; this.hashSecret=hashSecret;
    }

    static String nextStatus(String current, String action) {
        return switch(action) {
            case "hide" -> Set.of("PUBLISHED","PENDING_REVIEW").contains(current) ? "HIDDEN" : invalid();
            case "relist" -> Set.of("HIDDEN","ARCHIVED","EXPIRED","REJECTED").contains(current) ? "DRAFT" : invalid();
            case "sold" -> Set.of("PUBLISHED","HIDDEN").contains(current) ? "SOLD" : invalid();
            case "renew" -> "PUBLISHED".equals(current) ? "PUBLISHED" : "EXPIRED".equals(current) ? "PENDING_REVIEW" : invalid();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Thao tác tin đăng không hợp lệ.");
        };
    }
    private static String invalid() { throw new ResponseStatusException(HttpStatus.CONFLICT,"Thao tác không phù hợp trạng thái tin đăng."); }

    @Transactional
    public ListingResponse manage(String username, UUID id, String action) {
        Map<String,Object> row=owned(username,id);
        String status=nextStatus((String)row.get("status"),action);
        boolean active=Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from marketplace.transactions where listing_id=? and status not in ('COMPLETED','CANCELLED','REFUNDED'))",Boolean.class,row.get("id")));
        if(active) throw new ResponseStatusException(HttpStatus.CONFLICT,"Tin đang có giao dịch; cần xử lý giao dịch trước.");
        if("renew".equals(action)) {
            boolean recent=Boolean.TRUE.equals(jdbc.queryForObject("select renewed_at>now()-interval '1 day' from marketplace.vehicle_listings where id=?",Boolean.class,row.get("id")));
            if(recent) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Chỉ gia hạn một lần mỗi 24 giờ.");
            jdbc.update("update marketplace.vehicle_listings set status=?,expires_at=now()+interval '30 days',renewed_at=now(),updated_at=now(),version=version+1 where id=?",status,row.get("id"));
        } else jdbc.update("update marketplace.vehicle_listings set status=?,updated_at=now(),version=version+1 where id=?",status,row.get("id"));
        return listings.detail(id);
    }

    @Transactional(readOnly=true)
    public Map<String,Object> statistics(String username, UUID id) {
        var row=ownedRead(username,id);
        var result=new LinkedHashMap<String,Object>();
        result.putAll(jdbc.queryForMap("""
            select l.view_count as "viewCount",l.favorite_count as "favoriteCount",l.expires_at as "expiresAt",
             (select count(*) from marketplace.listing_engagements e where e.listing_id=l.id and e.event_type='CONTACT') as "contactCount",
             (select count(*) from marketplace.offers o where o.listing_id=l.id) as "offerCount"
            from marketplace.vehicle_listings l where l.id=?
            """,row.get("id")));
        result.put("priceHistory",jdbc.queryForList("select previous_price as \"previousPrice\",price,created_at as \"createdAt\" from marketplace.listing_price_history where listing_id=? order by id desc",row.get("id")));
        return result;
    }

    @Transactional
    public void engagement(UUID id, String username, String kind, String clientAddress, String agent) {
        if(kind==null || !Set.of("VIEW","CONTACT").contains(kind)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var rows=jdbc.queryForList("select l.id,l.seller_id,u.username from marketplace.vehicle_listings l join ebike_auth.users u on u.id=l.seller_id where l.public_id=? and l.status in ('PUBLISHED','RESERVED','SOLD','EXCHANGED')",id);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var row=rows.get(0);
        if(Objects.equals(username,row.get("username"))) return;
        String identity=username!=null ? "user:"+username : "visitor:"+clientAddress+":"+Objects.toString(agent,"");
        String key;
        try {
            Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            key=HexFormat.of().formatHex(mac.doFinal(identity.getBytes(StandardCharsets.UTF_8)));
        } catch(java.security.GeneralSecurityException ex) { throw new IllegalStateException(ex); }
        int inserted=jdbc.update("insert into marketplace.listing_engagements(listing_id,visitor_key,event_type) values (?,?,?) on conflict do nothing",row.get("id"),key,kind);
        if(inserted>0 && "VIEW".equals(kind)) jdbc.update("update marketplace.vehicle_listings set view_count=view_count+1 where id=?",row.get("id"));
    }

    @Scheduled(fixedDelayString="${app.marketplace.expiry-check-ms:60000}")
    @Transactional
    public void expireListings() {
        jdbc.update("update marketplace.vehicle_listings set status='EXPIRED',updated_at=now(),version=version+1 where status='PUBLISHED' and expires_at<=now()");
    }
    private Map<String,Object> owned(String username,UUID id) { return findOwned(username,id,true); }
    private Map<String,Object> ownedRead(String username,UUID id) { return findOwned(username,id,false); }
    private Map<String,Object> findOwned(String username,UUID id,boolean lock) {
        var rows=jdbc.queryForList("select l.* from marketplace.vehicle_listings l join ebike_auth.users u on u.id=l.seller_id where l.public_id=? and u.username=?"+(lock?" for update of l":""),id,username);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy tin thuộc tài khoản của bạn.");
        return rows.get(0);
    }
}
