package com.ebike.supportModule.service;
import static com.ebike.marketplaceModule.service.TradePolicy.*;
import com.ebike.marketplaceModule.service.MarketplaceTransactionService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportThreadService {
 private final JdbcTemplate db;private final MarketplaceTransactionService users;
 public SupportThreadService(JdbcTemplate db,MarketplaceTransactionService users){this.db=db;this.users=users;}
 private Map<String,Object> access(String username,long id){long actor=users.actor(username);var ticket=users.one("select * from ebike_support.support_tickets where id=?",id);if(!Objects.equals(ticket.get("user_id"),actor)&&!users.staff(actor))throw forbidden();return ticket;}
 @Transactional(readOnly=true)
 public Object messages(String username,long id){access(username,id);return db.queryForList("select m.id,m.author_id as \"authorId\",u.username,m.content,m.created_at as \"createdAt\",exists(select 1 from ebike_auth.user_roles ur join ebike_auth.roles r on r.id=ur.role_id where ur.user_id=u.id and r.name in ('ADMIN','MANAGER')) as staff from ebike_support.ticket_messages m join ebike_auth.users u on u.id=m.author_id where m.ticket_id=? order by m.id",id);}
 @Transactional
 public void reply(String username,long id,String content){var ticket=access(username,id);long actor=users.actor(username);if("CLOSED".equals(ticket.get("status")))throw conflict("Ticket đã đóng, hãy tạo yêu cầu mới.");if(content==null||content.isBlank()||content.length()>4000)throw bad("Nội dung từ 1 đến 4000 ký tự.");
  if(db.queryForObject("select count(*) from ebike_support.ticket_messages where author_id=? and created_at>now()-interval '1 minute'",Integer.class,actor)>=10)throw conflict("Bạn phản hồi quá nhanh.");
  db.update("insert into ebike_support.ticket_messages(ticket_id,author_id,content) values (?,?,?)",id,actor,content.trim());
  db.update("update ebike_support.support_tickets set status=?,updated_at=now() where id=?",users.staff(actor)?"WAITING_CUSTOMER":"IN_PROGRESS",id);
 }
}
