package com.ebike.marketplaceModule.service;

import com.ebike.adminModule.entity.AdminAuditLog;
import com.ebike.adminModule.repository.AdminAuditLogRepository;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MarketplaceOperationsService {
    private final JdbcTemplate jdbc;
    private final AdminAuditLogRepository audit;
    public MarketplaceOperationsService(JdbcTemplate jdbc, AdminAuditLogRepository audit) { this.jdbc=jdbc; this.audit=audit; }

    public Map<String,Object> overview() {
        return one("""
            select (select count(*) from marketplace.vehicle_listings where status='PENDING_REVIEW') as "pendingListings",
            (select count(*) from marketplace.vehicle_listings where status='PUBLISHED') as "activeListings",
            (select count(*) from marketplace.listing_reports where status='OPEN') as "openReports",
            (select count(*) from marketplace.transactions where status not in ('COMPLETED','CANCELLED','REFUNDED')) as "activeTransactions",
            (select count(*) from marketplace.transactions where status='DISPUTED') as "disputedTransactions",
            (select count(*) from marketplace.transactions where status='COMPLETED') as "completedTransactions",
            (select coalesce(sum(platform_fee),0) from marketplace.transactions where status='COMPLETED') as "platformFees",
            (select count(*) from marketplace.vehicle_listings where created_at >= current_date) as "newListings"
            """);
    }
    public List<Map<String,Object>> listings() {
        return jdbc.query("""
            select l.public_id as "publicId", l.title, l.description, l.price, l.status, l.seller_id as "sellerId",
                u.username as "sellerUsername", concat_ws(' ',u.first_name,u.last_name) as "sellerName",
                l.province, l.district, l.condition, l.listing_type as "listingType", c.name as "categoryName",
                d.manufacture_year as "manufactureYear", d.mileage_km as "mileageKm",
                l.created_at as "createdAt",
                (select array_agg(i.public_url order by i.primary_image desc,i.sort_order) from marketplace.vehicle_images i where i.listing_id=l.id) as images,
                (select count(*) from marketplace.listing_reports r where r.listing_id=l.id and r.status='OPEN') as "reportCount",
                (select m.note from marketplace.listing_moderations m where m.listing_id=l.id and m.reason_code is distinct from 'USER_REPORT' order by m.id desc limit 1) as "moderationNote"
            from marketplace.vehicle_listings l join ebike_auth.users u on u.id=l.seller_id
            join marketplace.vehicle_categories c on c.id=l.category_id
            left join marketplace.vehicle_details d on d.listing_id=l.id
            where l.status <> 'DRAFT'
            order by case when l.status='PENDING_REVIEW' then 0 else 1 end, l.updated_at desc
            """, (rs, index) -> {
                Map<String,Object> row=new LinkedHashMap<>();
                var meta=rs.getMetaData();
                for(int i=1;i<=meta.getColumnCount();i++) {
                    Object value=rs.getObject(i);
                    if(value instanceof java.sql.Array array) value=array.getArray();
                    row.put(meta.getColumnLabel(i),value);
                }
                if(row.get("images")==null) row.put("images",new String[0]);
                return row;
            });
    }
    @Transactional
    public Map<String,Object> moderate(String username, UUID id, Map<String,String> body) {
        long actor=actor(username);
        Map<String,Object> listing=one("select * from marketplace.vehicle_listings where public_id=? for update", id);
        String action=body.getOrDefault("action","");
        String next=OperationsPolicy.transition(String.valueOf(listing.get("status")),action,actor==number(listing.get("seller_id")),body.get("reason"),body.get("note"));
        if ("PUBLISHED".equals(next)) {
            Long count=jdbc.queryForObject("select count(*) from marketplace.vehicle_images where listing_id=? and moderation_status='APPROVED'",Long.class,listing.get("id"));
            if (count==null || count==0) throw bad("Tin cần có ảnh hợp lệ trước khi duyệt");
        }
        jdbc.update("update marketplace.vehicle_listings set status=?, published_at=case when ?='PUBLISHED' then now() else published_at end, updated_at=now(), version=version+1 where id=?", next,next,listing.get("id"));
        String decision=switch(next) { case "PUBLISHED" -> "APPROVED"; case "REJECTED" -> "REJECTED"; case "SUSPENDED" -> "SUSPENDED"; default -> "PENDING"; };
        jdbc.update("insert into marketplace.listing_moderations(listing_id,reviewer_id,decision,reason_code,note) values (?,?,?,?,?)",listing.get("id"),actor,decision,body.get("reason"),action+": "+body.getOrDefault("note",""));
        log(username,"LISTING_"+action,id+" | "+body.getOrDefault("note",""));
        return Map.of("status",next);
    }
    public List<Map<String,Object>> reports() {
        return jdbc.queryForList("""
            select r.id, l.public_id as "listingPublicId",l.title as "listingTitle",l.status as "listingStatus",
            u.username as reporter,r.reporter_id as "reporterId",r.reason,r.status,r.resolution,r.resolution_note as "resolutionNote",
            r.created_at as "createdAt", r.resolved_at as "resolvedAt", reviewer.username as "resolvedBy"
            from marketplace.listing_reports r join marketplace.vehicle_listings l on l.id=r.listing_id
            join ebike_auth.users u on u.id=r.reporter_id left join ebike_auth.users reviewer on reviewer.id=r.resolved_by
            order by case when r.status='OPEN' then 0 else 1 end,r.created_at desc
            """);
    }
    @Transactional
    public Map<String,Object> resolve(String username, Long id, Map<String,String> body) {
        long actor=actor(username);
        Map<String,Object> report=one("select * from marketplace.listing_reports where id=? for update",id);
        if (!"OPEN".equals(report.get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Báo cáo đã được xử lý");
        if (actor==number(report.get("reporter_id"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Không tự xử lý báo cáo của mình");
        Map<String,Object> listing=one("select public_id,seller_id from marketplace.vehicle_listings where id=?", report.get("listing_id"));
        if (actor==number(listing.get("seller_id"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Không tự xử lý báo cáo về tin của mình");
        String action=body.getOrDefault("action","");
        String note=body.get("note");
        requireNote(note);
        if (!Set.of("DISMISS","HIDE").contains(action)) throw bad("Chọn bỏ qua hoặc ẩn tin vi phạm");
        if ("HIDE".equals(action)) moderate(username,(UUID)listing.get("public_id"),Map.of("action","HIDE","reason","SUSPICIOUS_CONTENT","note",note));
        String status="DISMISS".equals(action)?"DISMISSED":"RESOLVED";
        jdbc.update("update marketplace.listing_reports set status=?,resolution=?,resolution_note=?,resolved_by=?,resolved_at=now() where id=?",status,action,note,actor,id);
        log(username,"REPORT_"+action,id+" | "+note);
        return Map.of("status",status);
    }
    public List<Map<String,Object>> transactions() {
        return jdbc.queryForList("""
            select t.public_id as "publicId",t.transaction_number as "transactionNumber",l.title as "listingTitle",
            t.buyer_id as "buyerId", t.seller_id as "sellerId", b.username as buyer,s.username as seller,
            t.agreed_price as "agreedPrice",t.deposit_amount as "depositAmount",t.status,t.created_at as "createdAt",
            (select jsonb_agg(jsonb_build_object('actor',u.username,'status',e.to_status,'note',e.note,'createdAt',e.created_at) order by e.created_at desc)
             from marketplace.transaction_events e left join ebike_auth.users u on u.id=e.actor_id where e.transaction_id=t.id)::text as history
            from marketplace.transactions t join marketplace.vehicle_listings l on l.id=t.listing_id
            join ebike_auth.users b on b.id=t.buyer_id join ebike_auth.users s on s.id=t.seller_id order by t.created_at desc
            """);
    }
    @Transactional
    public Map<String,Object> dispute(String username, UUID id, String note) {
        requireNote(note);
        long actor=actor(username);
        Map<String,Object> tx=one("select * from marketplace.transactions where public_id=? for update",id);
        if(actor==number(tx.get("buyer_id")) || actor==number(tx.get("seller_id"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Không tự xử lý giao dịch của mình");
        String status=String.valueOf(tx.get("status"));
        if (Set.of("COMPLETED","CANCELLED","REFUNDED","DISPUTED").contains(status)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể mở tranh chấp ở trạng thái này");
        jdbc.update("update marketplace.transactions set status='DISPUTED',updated_at=now(),version=version+1 where id=?",tx.get("id"));
        jdbc.update("insert into marketplace.transaction_events(transaction_id,actor_id,from_status,to_status,event_type,note) values (?,?,?,'DISPUTED','STAFF_OPENED_DISPUTE',?)",tx.get("id"),actor,status,note);
        log(username,"TRANSACTION_DISPUTED",id+" | "+note);
        return Map.of("status","DISPUTED");
    }
    @Transactional
    public Map<String,Object> resume(String username, UUID id, String note) {
        requireNote(note);
        long actor=actor(username);
        Map<String,Object> tx=one("select * from marketplace.transactions where public_id=? for update",id);
        if(actor==number(tx.get("buyer_id")) || actor==number(tx.get("seller_id"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Không tự xử lý giao dịch của mình");
        if(!"DISPUTED".equals(tx.get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Giao dịch không đang tranh chấp");
        Map<String,Object> event=one("select from_status from marketplace.transaction_events where transaction_id=? and event_type='STAFF_OPENED_DISPUTE' order by id desc limit 1",tx.get("id"));
        String previous=String.valueOf(event.get("from_status"));
        if(java.util.Set.of("DISPUTED","COMPLETED","CANCELLED","REFUNDED").contains(previous)) throw bad("Không thể khôi phục trạng thái này");
        jdbc.update("update marketplace.transactions set status=?,updated_at=now(),version=version+1 where id=?",previous,tx.get("id"));
        jdbc.update("insert into marketplace.transaction_events(transaction_id,actor_id,from_status,to_status,event_type,note) values (?,?,'DISPUTED',?,'STAFF_RESOLVED_DISPUTE',?)",tx.get("id"),actor,previous,note);
        log(username,"TRANSACTION_RESUMED",id+" | "+note);
        return Map.of("status",previous);
    }

    private void log(String actor,String action,String target) {
        AdminAuditLog entry=new AdminAuditLog(); entry.setActor(actor);entry.setAction(action);entry.setTarget(target.length()>255 ? target.substring(0,255) : target);entry.setIpAddress("system");audit.save(entry);
    }
    private long actor(String username) { return number(one("select id from ebike_auth.users where username=? and is_active=true",username).get("id")); }
    private long number(Object value) { return ((Number)value).longValue(); }
    private void requireNote(String note) { if(note==null || note.isBlank() || note.length()>2000) throw bad("Nhập ghi chú xử lý (tối đa 2000 ký tự)"); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    private Map<String,Object> one(String sql,Object... args) {
        List<Map<String,Object>> rows=jdbc.queryForList(sql,args);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy dữ liệu");
        return rows.get(0);
    }
}
