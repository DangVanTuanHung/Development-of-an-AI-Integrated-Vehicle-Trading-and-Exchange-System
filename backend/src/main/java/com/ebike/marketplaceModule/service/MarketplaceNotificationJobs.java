package com.ebike.marketplaceModule.service;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ebike.notificationModule.service.NotificationStreamService;

@Service
public class MarketplaceNotificationJobs {
 private final JdbcTemplate db;private final MarketplaceCommunityService community;private final MarketplaceDiscoveryService discovery;
 private final NotificationStreamService stream;private final JavaMailSender mail;private final String host,from;
 public MarketplaceNotificationJobs(JdbcTemplate db,MarketplaceCommunityService community,MarketplaceDiscoveryService discovery,NotificationStreamService stream,JavaMailSender mail,@Value("${spring.mail.host:}") String host,@Value("${app.auth.verification.from:no-reply@motionx.local}") String from){this.db=db;this.community=community;this.discovery=discovery;this.stream=stream;this.mail=mail;this.host=host;this.from=from;}
 @Scheduled(fixedDelayString="${app.marketplace.notifications.delivery-ms:2000}")
 public void deliver(){
  for(var row:db.queryForList("select n.id,n.user_id,n.title,n.message,n.target_url,d.email_requested,d.email_status,d.attempts,u.email,u.email_verified_at from marketplace.notification_delivery d join ebike_user.notifications n on n.id=d.notification_id join ebike_auth.users u on u.id=n.user_id where (not d.streamed or (d.email_status='PENDING' and d.next_attempt_at<=now())) order by n.id limit 100")){
   long id=((Number)row.get("id")).longValue();stream.sendUnreadCount(((Number)row.get("user_id")).longValue());
   db.update("update marketplace.notification_delivery set streamed=true where notification_id=?",id);
   if(!"PENDING".equals(row.get("email_status")))continue;
   boolean local=Set.of("mailpit","localhost","127.0.0.1").contains(host);
   if(!Boolean.TRUE.equals(row.get("email_requested"))||host.isBlank()||row.get("email")==null||(!local&&row.get("email_verified_at")==null)){
    db.update("update marketplace.notification_delivery set email_status='SKIPPED' where notification_id=?",id);continue;
   }
   try{var message=new SimpleMailMessage();message.setFrom(from);message.setTo((String)row.get("email"));message.setSubject("MOTIONX - "+row.get("title"));message.setText(row.get("message")+"\nMở ứng dụng MOTIONX: "+row.get("target_url"));mail.send(message);db.update("update marketplace.notification_delivery set email_status='SENT',attempts=attempts+1 where notification_id=?",id);}
   catch(org.springframework.mail.MailException ex){db.update("update marketplace.notification_delivery set attempts=attempts+1,email_status=case when attempts>=2 then 'FAILED' else 'PENDING' end,next_attempt_at=now()+interval '1 minute' where notification_id=?",id);}
  }
 }
 @Scheduled(fixedDelayString="${app.marketplace.notifications.scan-ms:10000}") @Transactional
 public void scan(){
  for(String kind:List.of("APPOINTMENT","HANDOVER")){
   String query=kind.equals("APPOINTMENT")?"select id,public_id,buyer_id,seller_id,revision,scheduled_at as at from marketplace.appointments where status='CONFIRMED' and scheduled_at>now() and scheduled_at<=now()+interval '1 hour'":"select id,public_id,buyer_id,seller_id,handover_revision as revision,handover_at as at from marketplace.transactions where status in ('DEPOSIT_PENDING','DEPOSIT_PAID','FINAL_PAYMENT_PENDING','INSPECTION_PENDING','FULLY_PAID') and handover_at>now() and handover_at<=now()+interval '1 hour' and buyer_plan_agreed_at is not null and seller_plan_agreed_at is not null";
   for(var row:db.queryForList(query)){
    String key=kind+":"+row.get("id")+":"+row.get("revision");
    if(db.update("insert into marketplace.reminders(event_key) values (?) on conflict do nothing",key)==0)continue;
    String url=kind.equals("APPOINTMENT")?"/appointments":"/deals/"+row.get("public_id");
    for(String role:List.of("buyer_id","seller_id"))community.notify(((Number)row.get(role)).longValue(),"Nhắc lịch sắp đến","Bạn có lịch "+(kind.equals("APPOINTMENT")?"xem xe":"bàn giao")+" trong vòng một giờ.",url,true);
   }
  }
  for(var search:db.queryForList("select id,user_id,name,filters::text,created_at from marketplace.saved_searches order by id")){
   for(UUID publicId:discovery.newMatches(discovery.decode((String)search.get("filters")),search.get("created_at"),search.get("id"))){
    var listing=db.queryForMap("select id,title from marketplace.vehicle_listings where public_id=?",publicId);
    if(db.update("insert into marketplace.search_alerts(search_id,listing_id) values (?,?) on conflict do nothing",search.get("id"),listing.get("id"))>0)community.notify(((Number)search.get("user_id")).longValue(),"Xe phù hợp tìm kiếm đã lưu",search.get("name")+": "+listing.get("title"),"/listing/"+publicId,false);
   }
   // Search all unseen matches since creation, so late commits and batches over
   // 100 rows are picked up on the next run instead of falling behind a watermark.
   db.update("update marketplace.saved_searches set checked_at=now() where id=?",search.get("id"));
  }
 }
}
