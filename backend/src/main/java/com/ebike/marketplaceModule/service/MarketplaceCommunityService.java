package com.ebike.marketplaceModule.service;

import static com.ebike.marketplaceModule.service.TradePolicy.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceCommunityService {
    private final JdbcTemplate db;
    private final MarketplaceTransactionService users;
    public MarketplaceCommunityService(JdbcTemplate db,MarketplaceTransactionService users){this.db=db;this.users=users;}
    private long n(Object value){return ((Number)value).longValue();}
    private Map<String,Object> one(String sql,Object...args){return users.one(sql,args);}
    public void notify(long user,String title,String message,String url,boolean email){db.queryForObject("select marketplace.notify(?,?,?,?,?)",Object.class,user,title,message,url,email);}
    public boolean blocked(long a,long b){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from marketplace.user_blocks where (user_id=? and blocked_id=?) or (user_id=? and blocked_id=?))",Boolean.class,a,b,b,a));}
    public void canContact(long a,long b){if(blocked(a,b))throw forbidden();}
    private String text(String value,int max){if(value==null||value.isBlank()||value.length()>max)throw bad("Nội dung bắt buộc, tối đa "+max+" ký tự.");return value.trim();}
    private Map<String,Object> visible(UUID id,Long actor){
        var listing=one("select * from marketplace.vehicle_listings where public_id=?",id);
        if(!Set.of("PUBLISHED","RESERVED","SOLD","EXCHANGED").contains(listing.get("status"))&&(actor==null||(actor!=n(listing.get("seller_id"))&&!users.staff(actor))))throw forbidden();
        return listing;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> comments(UUID listing,String username,int page){
        Long actor=username==null?null:users.actor(username);var car=visible(listing,actor);int offset=Math.max(0,Math.min(page,1000))*10;
        var roots=db.queryForList("select id from marketplace.listing_comments where listing_id=? and parent_id is null order by created_at desc,id desc limit 11 offset ?",Long.class,car.get("id"),offset);
        boolean more=roots.size()>10;if(more)roots=roots.subList(0,10);
        if(roots.isEmpty())return Map.of("items",List.of(),"hasMore",false);
        String placeholders=String.join(",",Collections.nCopies(roots.size(),"?"));var args=new ArrayList<Object>(roots);args.addAll(roots);
        var items=db.queryForList("select c.public_id as \"publicId\",p.public_id as \"parentId\",c.author_id as \"authorId\",u.username,c.status,case when c.status='VISIBLE' then c.content else 'Bình luận đã được ẩn hoặc xóa.' end as content,c.created_at as \"createdAt\",c.updated_at as \"updatedAt\",(select count(*) from marketplace.listing_comments r where r.parent_id=c.id) as \"replyCount\" from marketplace.listing_comments c join ebike_auth.users u on u.id=c.author_id left join marketplace.listing_comments p on p.id=c.parent_id where c.id in ("+placeholders+") or c.parent_id in ("+placeholders+") order by c.created_at,c.id",args.toArray());
        return Map.of("items",items,"hasMore",more);
    }
    @Transactional
    public void addComment(String username,UUID listing,UUID parent,String content){
        long actor=users.actor(username);var car=visible(listing,actor);canContact(actor,n(car.get("seller_id")));
        if(db.queryForObject("select count(*) from marketplace.listing_comments where author_id=? and created_at>now()-interval '1 minute'",Integer.class,actor)>=10)throw conflict("Bạn bình luận quá nhanh. Hãy chờ một phút.");
        Long parentId=null;Long replyTo=null;
        if(parent!=null){var p=one("select * from marketplace.listing_comments where public_id=? and listing_id=?",parent,car.get("id"));if(!"VISIBLE".equals(p.get("status")))throw conflict("Bình luận đã ẩn hoặc xóa.");replyTo=n(p.get("author_id"));canContact(actor,replyTo);parentId=p.get("parent_id")==null?n(p.get("id")):n(p.get("parent_id"));}
        db.update("insert into marketplace.listing_comments(listing_id,author_id,parent_id,content) values (?,?,?,?)",car.get("id"),actor,parentId,text(content,2000));
        String url="/listing/"+listing;
        if(actor!=n(car.get("seller_id")))notify(n(car.get("seller_id")),"Bình luận mới", "Có bình luận mới trên tin "+car.get("title"),url,false);
        if(replyTo!=null&&replyTo!=actor&&replyTo!=n(car.get("seller_id")))notify(replyTo,"Trả lời bình luận","Có phản hồi mới cho bình luận của bạn.",url,false);
    }
    @Transactional
    public void commentAction(String username,UUID id,String action,String content){
        long actor=users.actor(username);var c=one("select c.*,l.seller_id from marketplace.listing_comments c join marketplace.vehicle_listings l on l.id=c.listing_id where c.public_id=? for update of c",id);
        boolean author=actor==n(c.get("author_id"));boolean owner=actor==n(c.get("seller_id"));boolean staff=users.staff(actor);
        if("edit".equals(action)){if(!author||!"VISIBLE".equals(c.get("status")))throw forbidden();db.update("update marketplace.listing_comments set content=?,updated_at=now() where id=?",text(content,2000),c.get("id"));}
        else {if(!(author||owner||staff))throw forbidden();if(!Set.of("hide","delete","restore").contains(action))throw bad("Thao tác không hợp lệ.");if("restore".equals(action)&&(!staff||"DELETED".equals(c.get("status"))))throw forbidden();db.update("update marketplace.listing_comments set status=?,updated_at=now() where id=?",switch(action){case "hide"->"HIDDEN";case "delete"->"DELETED";default->"VISIBLE";},c.get("id"));}
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> blocks(String username){return db.queryForList("select b.blocked_id as id,u.username,b.created_at as \"createdAt\" from marketplace.user_blocks b join ebike_auth.users u on u.id=b.blocked_id where b.user_id=? order by b.created_at desc",users.actor(username));}
    @Transactional
    public void block(String username,long target,boolean enabled){long actor=users.actor(username);one("select id from ebike_auth.users where id=?",target);if(actor==target)throw bad("Không thể chặn chính mình.");if(enabled)db.update("insert into marketplace.user_blocks(user_id,blocked_id) values (?,?) on conflict do nothing",actor,target);else db.update("delete from marketplace.user_blocks where user_id=? and blocked_id=?",actor,target);}
    @Transactional
    public void report(String username,String type,String target,String reason,String description){
        long actor=users.actor(username);type=Objects.toString(type,"").toUpperCase(Locale.ROOT);
        long targetUser;String snapshot;
        switch(type){
            case "COMMENT"->{var c=one("select c.*,l.public_id as listing from marketplace.listing_comments c join marketplace.vehicle_listings l on l.id=c.listing_id where c.public_id=? and c.status='VISIBLE'",uuid(target));visible((UUID)c.get("listing"),actor);targetUser=n(c.get("author_id"));snapshot=(String)c.get("content");}
            case "REVIEW"->{var r=one("select * from marketplace.seller_reviews where public_id=? and status='VISIBLE'",uuid(target));targetUser=n(r.get("reviewer_id"));snapshot=Objects.toString(r.get("comment"),"")+" ("+r.get("rating")+" sao)";}
            case "MESSAGE"->{var m=one("select m.* from marketplace.messages m join marketplace.conversation_members cm on cm.conversation_id=m.conversation_id where m.public_id=? and cm.user_id=?",uuid(target),actor);targetUser=n(m.get("sender_id"));snapshot=Objects.toString(m.get("content"),"Tệp đính kèm");}
            case "USER"->{long id;try{id=Long.parseLong(target);}catch(Exception ex){throw bad("Tài khoản không hợp lệ.");}var u=one("select id,username from ebike_auth.users where id=?",id);targetUser=n(u.get("id"));snapshot=(String)u.get("username");}
            default->throw bad("Loại báo cáo không hợp lệ.");
        }
        if(targetUser==actor)throw bad("Không báo cáo chính mình.");
        if(db.queryForObject("select count(*) from marketplace.community_reports where reporter_id=? and created_at>now()-interval '1 minute'",Integer.class,actor)>=10)throw conflict("Bạn gửi báo cáo quá nhanh.");
        int inserted=db.update("insert into marketplace.community_reports(reporter_id,target_type,target_id,target_user_id,reason,description,snapshot) values (?,?,?,?,?,?,?) on conflict do nothing",actor,type,target,targetUser,text(reason,80),text(description,2000),snapshot);
        if(inserted==0)throw conflict("Báo cáo này đang chờ xử lý.");
    }
    private UUID uuid(String value){try{return UUID.fromString(value);}catch(Exception ex){throw bad("Mã nội dung không hợp lệ.");}}
    @Transactional(readOnly=true)
    public List<Map<String,Object>> reports(String username,boolean staff){
        long actor=users.actor(username);if(staff&&!users.staff(actor))throw forbidden();
        var result=db.queryForList("select r.id,r.target_type as \"targetType\",r.target_id as \"targetId\",r.target_user_id as \"targetUserId\",u.username as reporter,r.reason,r.description,r.snapshot,r.status,r.resolution,r.created_at as \"createdAt\",r.resolved_at as \"resolvedAt\" from marketplace.community_reports r join ebike_auth.users u on u.id=r.reporter_id "+(staff?"":"where r.reporter_id=? ")+"order by r.created_at desc",staff?new Object[]{}:new Object[]{actor});
        if(!staff)result.addAll(db.queryForList("select r.id,'LISTING' as \"targetType\",l.public_id::text as \"targetId\",l.seller_id as \"targetUserId\",u.username as reporter,r.reason,r.reason as description,l.title as snapshot,r.status,r.resolution_note as resolution,r.created_at as \"createdAt\",r.resolved_at as \"resolvedAt\" from marketplace.listing_reports r join marketplace.vehicle_listings l on l.id=r.listing_id join ebike_auth.users u on u.id=r.reporter_id where r.reporter_id=? order by r.created_at desc",actor));
        return result;
    }
    @Transactional
    public void resolveReport(String username,long id,String action,String note){
        long actor=users.actor(username);if(!users.staff(actor))throw forbidden();var r=one("select * from marketplace.community_reports where id=? for update",id);
        if(actor==n(r.get("reporter_id"))||actor==n(r.get("target_user_id")))throw forbidden();if(!"OPEN".equals(r.get("status")))throw conflict("Báo cáo đã xử lý.");
        String resolution=text(note,2000);if(!Set.of("DISMISS","WARN","HIDE","DELETE","LOCK").contains(Objects.toString(action,"")))throw bad("Thao tác không hợp lệ.");
        String type=(String)r.get("target_type");
        if(Set.of("HIDE","DELETE").contains(action)){
            String status="DELETE".equals(action)?"DELETED":"HIDDEN";
            switch(type){case "COMMENT"->db.update("update marketplace.listing_comments set status=?,updated_at=now() where public_id=?",status,uuid((String)r.get("target_id")));case "REVIEW"->db.update("update marketplace.seller_reviews set status=?,updated_at=now() where public_id=?",status,uuid((String)r.get("target_id")));case "MESSAGE"->db.update("update marketplace.messages set deleted_at=now() where public_id=?",uuid((String)r.get("target_id")));default->throw bad("Tài khoản chỉ có thể cảnh cáo hoặc khóa.");}
        }
        if("LOCK".equals(action))db.update("update ebike_auth.users set is_active=false,token_version=token_version+1 where id=?",r.get("target_user_id"));
        db.update("update marketplace.community_reports set status=?,resolution=?,resolved_by=?,resolved_at=now() where id=?","DISMISS".equals(action)?"DISMISSED":"RESOLVED",action+": "+resolution,actor,id);
        db.update("insert into ebike_admin.admin_audit_logs(actor,action,target,ip_address) values (?,'COMMUNITY_REPORT',?,'system')",username,id+" "+action+" "+resolution);
        notify(n(r.get("reporter_id")),"Kết quả báo cáo",action+": "+resolution,"/marketplace-account",true);
        if(!"DISMISS".equals(action))notify(n(r.get("target_user_id")),"Thông báo kiểm duyệt",resolution,"/marketplace-account",true);
    }
    @Transactional
    public void reviewReply(String username,UUID id,String reply){long actor=users.actor(username);var r=one("select * from marketplace.seller_reviews where public_id=? for update",id);if(actor!=n(r.get("seller_id"))||!"VISIBLE".equals(r.get("status")))throw forbidden();db.update("update marketplace.seller_reviews set reply=?,replied_at=now(),updated_at=now() where id=?",text(reply,2000),r.get("id"));notify(n(r.get("reviewer_id")),"Phản hồi đánh giá","Người được đánh giá đã phản hồi nhận xét của bạn.","/seller/"+actor,false);}
    @Transactional(readOnly=true)
    public List<Map<String,Object>> myComments(String username){return db.queryForList("select c.public_id as \"publicId\",c.content,c.status,l.title,l.public_id as \"listingPublicId\" from marketplace.listing_comments c join marketplace.vehicle_listings l on l.id=c.listing_id where c.author_id=? order by c.created_at desc limit 100",users.actor(username));}
    @Transactional(readOnly=true)
    public List<Map<String,Object>> myReviews(String username){long actor=users.actor(username);return db.queryForList("select r.public_id as \"publicId\",r.reviewer_id as \"reviewerId\",r.seller_id as \"targetId\",u.username as \"reviewerName\",v.username as \"targetName\",r.rating,r.comment,r.reply,r.status from marketplace.seller_reviews r join ebike_auth.users u on u.id=r.reviewer_id join ebike_auth.users v on v.id=r.seller_id where r.reviewer_id=? or r.seller_id=? order by r.created_at desc",actor,actor);}
}
