package com.ebike.marketplaceModule.service;

import static com.ebike.marketplaceModule.service.TradePolicy.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MarketplaceTransactionService {
    private final JdbcTemplate jdbc;
    public MarketplaceTransactionService(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public static long n(Object value){return ((Number)value).longValue();}
    public Map<String,Object> one(String sql,Object... args){var rows=jdbc.queryForList(sql,args);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);return rows.get(0);}
    public long actor(String username){return n(one("select id from ebike_auth.users where username=? and is_active",username).get("id"));}
    public boolean staff(long actor){return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from ebike_auth.user_roles ur join ebike_auth.roles r on r.id=ur.role_id where ur.user_id=? and r.name in ('ADMIN','MANAGER'))",Boolean.class,actor));}
    public boolean participant(Map<String,Object> tx,long actor){return n(tx.get("buyer_id"))==actor||n(tx.get("seller_id"))==actor;}
    public long payer(Map<String,Object> tx){return n(tx.get(((BigDecimal)tx.get("exchange_value")).compareTo((BigDecimal)tx.get("agreed_price"))>0?"seller_id":"buyer_id"));}
    public long payee(Map<String,Object> tx){return payer(tx)==n(tx.get("buyer_id"))?n(tx.get("seller_id")):n(tx.get("buyer_id"));}
    public Map<String,Object> read(UUID id){return one("select * from marketplace.transactions where public_id=?",id);}
    private Map<String,Object> locked(UUID id){return one("select * from marketplace.transactions where public_id=? for update",id);}
    private void own(Map<String,Object> tx,long actor){if(!participant(tx,actor))throw forbidden();}
    private void reviewer(Map<String,Object> tx,long actor){if(!staff(actor)||participant(tx,actor))throw forbidden();}
    public void event(Object tx,long actor,String from,String to,String type,String note){jdbc.update("insert into marketplace.transaction_events(transaction_id,actor_id,from_status,to_status,event_type,note) values (?,?,?,?,?,?)",tx,actor,from,to,type,note);}
    private void transition(Map<String,Object> tx,long actor,String next,String type,String note){
        jdbc.update("update marketplace.transactions set status=?,updated_at=now(),version=version+1 where id=?",next,tx.get("id"));
        event(tx.get("id"),actor,(String)tx.get("status"),next,type,note);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> mine(String username){long user=actor(username);return jdbc.queryForList(summary()+" where t.buyer_id=? or t.seller_id=? order by t.created_at desc",user,user);}
    @Transactional(readOnly=true)
    public List<Map<String,Object>> all(String username){if(!staff(actor(username)))throw forbidden();return jdbc.queryForList(summary()+" order by t.created_at desc");}
    private String summary(){return """
        select t.public_id as "publicId",t.transaction_number as "transactionNumber",l.title as "listingTitle",t.buyer_id as "buyerId",t.seller_id as "sellerId",
        b.username as "buyerName",s.username as "sellerName",t.agreed_price as "agreedPrice",t.exchange_value as "exchangeValue",t.payable_amount as "payableAmount",
        t.deposit_amount as "depositAmount",t.status,t.created_at as "createdAt"
        from marketplace.transactions t join marketplace.vehicle_listings l on l.id=t.listing_id
        join ebike_auth.users b on b.id=t.buyer_id join ebike_auth.users s on s.id=t.seller_id
        """;}
    @Transactional(readOnly=true)
    public Map<String,Object> detail(String username,UUID id){
        long user=actor(username);var tx=read(id);if(!participant(tx,user)&&!staff(user))throw forbidden();
        var result=new LinkedHashMap<String,Object>(one(summary()+" where t.id=?",tx.get("id")));
        result.put("listingPublicId",one("select public_id from marketplace.vehicle_listings where id=?",tx.get("listing_id")).get("public_id"));
        if(tx.get("exchange_listing_id")!=null)result.put("exchangeVehicle",one("select public_id as \"publicId\",title from marketplace.vehicle_listings where id=?",tx.get("exchange_listing_id")));
        result.put("payerId",payer(tx));result.put("payeeId",payee(tx));result.put("canReviewDispute",staff(user)&&!participant(tx,user));
        result.put("refundPolicy",tx.get("refund_policy"));result.put("cancellationRequestedBy",tx.get("cancellation_requested_by"));result.put("cancellationReason",tx.get("cancellation_reason"));
        result.put("payments",jdbc.queryForList("select public_id as \"publicId\",payment_reference as \"paymentReference\",payment_stage as stage,provider,amount,status,transfer_reference as \"transferReference\",submitted_at as \"submittedAt\",paid_at as \"paidAt\",expires_at as \"expiresAt\" from marketplace_payment.payments where transaction_id=? order by created_at desc",tx.get("id")));
        result.put("refunds",jdbc.queryForList("select r.public_id as \"publicId\",p.payment_reference as \"paymentReference\",r.amount,r.reason,r.status,r.transfer_reference as \"transferReference\",r.sent_at as \"sentAt\",r.received_at as \"receivedAt\" from marketplace_payment.refunds r join marketplace_payment.payments p on p.id=r.payment_id where p.transaction_id=? order by r.id",tx.get("id")));
        var plan=new LinkedHashMap<String,Object>();
        plan.put("scheduledAt",tx.get("handover_at"));plan.put("location",tx.get("handover_location"));plan.put("note",tx.get("handover_note"));plan.put("revision",tx.get("handover_revision"));
        plan.put("buyerAgreedAt",tx.get("buyer_plan_agreed_at"));plan.put("sellerAgreedAt",tx.get("seller_plan_agreed_at"));plan.put("buyerConfirmedAt",tx.get("buyer_handover_confirmed_at"));plan.put("sellerConfirmedAt",tx.get("seller_handover_confirmed_at"));result.put("handover",plan);
        result.put("evidence",jdbc.queryForList("select public_id as \"publicId\",file_name as \"fileName\",mime_type as \"mimeType\",file_size as size,uploaded_by as \"uploadedBy\",created_at as \"createdAt\" from marketplace.transaction_evidence where transaction_id=? order by id",tx.get("id")));
        result.put("disputes",jdbc.queryForList("select public_id as \"publicId\",opened_by as \"openedBy\",description,status,resolution,created_at as \"createdAt\",resolved_at as \"resolvedAt\" from marketplace.disputes where transaction_id=? order by id desc",tx.get("id")));
        result.put("history",jdbc.queryForList("select e.event_type as type,e.from_status as \"fromStatus\",e.to_status as \"toStatus\",e.note,u.username as actor,e.created_at as \"createdAt\" from marketplace.transaction_events e left join ebike_auth.users u on u.id=e.actor_id where e.transaction_id=? order by e.id desc",tx.get("id")));
        return result;
    }
    @Transactional
    public Map<String,Object> createPayment(String username,UUID id,String stage,String provider){
        long user=actor(username);var tx=locked(id);if(user!=payer(tx))throw forbidden();active((String)tx.get("status"));
        if("VNPAY".equals(provider))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"VNPay chưa được kết nối cho luồng này. Có thể chọn chuyển khoản hoặc tiền mặt.");
        if(!Set.of("BANK_TRANSFER","CASH_ON_DELIVERY").contains(provider)||!Set.of("DEPOSIT","FINAL").contains(stage))throw bad("Phương thức/giai đoạn thanh toán không hợp lệ.");
        String status=(String)tx.get("status");boolean cash="CASH_ON_DELIVERY".equals(provider);
        var pending=jdbc.queryForList("select *,expires_at>now() as valid from marketplace_payment.payments where transaction_id=? and status in ('PENDING','PROCESSING') order by id desc",tx.get("id"));
        for(var p:pending){
            if("PROCESSING".equals(p.get("status"))||Boolean.TRUE.equals(p.get("valid"))){
                if(stage.equals(p.get("payment_stage"))&&provider.equals(p.get("provider")))return detail(username,id);
                throw conflict("Đã có khoản thanh toán đang chờ. Hãy xử lý khoản đó trước.");
            }
            jdbc.update("update marketplace_payment.payments set status='CANCELLED',updated_at=now() where id=?",p.get("id"));
        }
        if("DEPOSIT".equals(stage)&&(!"DEPOSIT_PENDING".equals(status)||cash))throw conflict("Giao dịch không chờ chuyển khoản tiền cọc.");
        if("FINAL".equals(stage)&&!Set.of("DEPOSIT_PAID","INSPECTION_PENDING","INSPECTION_PASSED","FINAL_PAYMENT_PENDING").contains(status)&&!(cash&&"DEPOSIT_PENDING".equals(status)))throw conflict("Giao dịch chưa sẵn sàng thanh toán phần còn lại.");
        BigDecimal paid=jdbc.queryForObject("select coalesce(sum(amount),0) from marketplace_payment.payments where transaction_id=? and status='PAID'",BigDecimal.class,tx.get("id"));
        BigDecimal amount="DEPOSIT".equals(stage)?(BigDecimal)tx.get("deposit_amount"):((BigDecimal)tx.get("payable_amount")).subtract(paid);
        if(amount.signum()<=0)throw conflict("Không còn số tiền cần thanh toán.");
        UUID payment=UUID.randomUUID();
        jdbc.update("insert into marketplace_payment.payments(public_id,transaction_id,payment_reference,payment_stage,provider,amount,idempotency_key,expires_at) values (?,?,?,?,?,?,?,now()+interval '30 minutes')",payment,tx.get("id"),"PAY-"+payment.toString().substring(0,18),stage,provider,amount,payment.toString());
        if(cash&&"DEPOSIT_PENDING".equals(status))transition(tx,user,"INSPECTION_PENDING","CASH_SELECTED","Hai bên sẽ xác nhận việc giao và nhận tiền mặt.");
        else event(tx.get("id"),user,status,status,"PAYMENT_REQUESTED","Tạo yêu cầu thanh toán "+stage);
        return detail(username,id);
    }
    private Map<String,Object> txForPayment(UUID payment){return read((UUID)one("select t.public_id from marketplace.transactions t join marketplace_payment.payments p on p.transaction_id=t.id where p.public_id=?",payment).get("public_id"));}
    @Transactional
    public Map<String,Object> payment(String username,UUID payment,String action,String reference){
        var tx=txForPayment(payment);tx=locked((UUID)tx.get("public_id"));long user=actor(username);active((String)tx.get("status"));
        var p=one("select *,expires_at>now() as valid from marketplace_payment.payments where public_id=? for update",payment);
        if("VNPAY".equals(p.get("provider")))throw bad("Thanh toán cổng chỉ được xác nhận bằng callback hợp lệ.");
        if("sent".equals(action)){
            if(user!=payer(tx))throw forbidden();
            if(!"PENDING".equals(p.get("status"))||!Boolean.TRUE.equals(p.get("valid")))throw conflict("Yêu cầu đã xử lý hoặc hết hạn. Hãy tạo lại nếu chưa chuyển tiền.");
            String ref=reference(reference);jdbc.update("update marketplace_payment.payments set status='PROCESSING',submitted_at=now(),transfer_reference=?,updated_at=now() where id=?",ref,p.get("id"));
            event(tx.get("id"),user,(String)tx.get("status"),(String)tx.get("status"),"PAYER_REPORTED_SENT","Bên trả tiền báo đã chuyển/giao tiền. Mã: "+ref);
        }else if("confirm".equals(action)){
            if(user!=payee(tx))throw forbidden();if(!"PROCESSING".equals(p.get("status")))throw conflict("Bên trả tiền chưa báo đã chuyển/giao tiền, hoặc khoản này đã được xử lý.");
            jdbc.update("update marketplace_payment.payments set status='PAID',paid_at=now(),updated_at=now() where id=?",p.get("id"));
            BigDecimal total=jdbc.queryForObject("select coalesce(sum(amount),0) from marketplace_payment.payments where transaction_id=? and status='PAID'",BigDecimal.class,tx.get("id"));
            transition(tx,user,total.compareTo((BigDecimal)tx.get("payable_amount"))>=0?"FULLY_PAID":"DEPOSIT_PAID","PAYMENT_RECEIVED","Bên nhận xác nhận đã nhận tiền; chưa đồng nghĩa đã bàn giao xe.");
        }else if("cancel".equals(action)){
            if(user!=payer(tx))throw forbidden();if(!"PENDING".equals(p.get("status")))throw conflict("Chỉ hủy yêu cầu chưa báo chuyển tiền.");
            jdbc.update("update marketplace_payment.payments set status='CANCELLED',updated_at=now() where id=?",p.get("id"));
            event(tx.get("id"),user,(String)tx.get("status"),(String)tx.get("status"),"PAYMENT_CANCELLED","Hủy yêu cầu thanh toán chưa gửi tiền.");
        }else throw bad("Thao tác không hợp lệ.");
        return detail(username,(UUID)tx.get("public_id"));
    }
    private String reference(String value){if(value==null||value.isBlank()||value.length()>160)throw bad("Nhập mã chuyển tiền/biên nhận từ 1 đến 160 ký tự.");return value.trim();}
    @Transactional
    public Map<String,Object> cancellation(String username,UUID id,String action,String reason){
        long user=actor(username);var tx=locked(id);own(tx,user);String status=(String)tx.get("status");
        if("request".equals(action)){
            active(status);String note=note(reason);ensureNoUnconfirmedFunds(tx);
            jdbc.update("update marketplace.transactions set cancellation_requested_by=?,cancellation_reason=?,cancellation_previous_status=? where id=?",user,note,status,tx.get("id"));
            transition(tx,user,"CANCELLATION_REQUESTED","CANCELLATION_REQUESTED",note);
        }else{
            if(!"CANCELLATION_REQUESTED".equals(status))throw conflict("Giao dịch không chờ phản hồi hủy.");
            boolean requester=tx.get("cancellation_requested_by")!=null&&n(tx.get("cancellation_requested_by"))==user;
            if("withdraw".equals(action)?!requester:requester)throw forbidden();
            if("approve".equals(action))finishCancellation(tx,user,"Hai bên thống nhất hủy và hoàn toàn bộ tiền đã xác nhận nhận.");
            else if(Set.of("reject","withdraw").contains(action))transition(tx,user,(String)tx.get("cancellation_previous_status"),"CANCELLATION_"+action.toUpperCase(Locale.ROOT),"Không tiếp tục yêu cầu hủy.");
            else throw bad("Thao tác không hợp lệ.");
        }
        return detail(username,id);
    }
    private void ensureNoUnconfirmedFunds(Map<String,Object> tx){if(Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from marketplace_payment.payments where transaction_id=? and status='PROCESSING')",Boolean.class,tx.get("id"))))throw conflict("Có khoản đã báo chuyển nhưng chưa được xác nhận. Hãy xác minh khoản tiền hoặc mở tranh chấp trước.");}
    private void finishCancellation(Map<String,Object> tx,long user,String reason){
        ensureNoUnconfirmedFunds(tx);
        jdbc.update("update marketplace_payment.payments set status='CANCELLED',updated_at=now() where transaction_id=? and status='PENDING'",tx.get("id"));
        for(var p:jdbc.queryForList("select id,amount from marketplace_payment.payments where transaction_id=? and status='PAID' order by id",tx.get("id")))
            jdbc.update("insert into marketplace_payment.refunds(payment_id,requested_by,amount,reason,status,reviewed_by,reviewed_at) values (?,?,?,?,'APPROVED',?,now()) on conflict do nothing",p.get("id"),user,p.get("amount"),reason,user);
        int count=jdbc.queryForObject("select count(*) from marketplace_payment.refunds r join marketplace_payment.payments p on p.id=r.payment_id where p.transaction_id=? and r.status in ('APPROVED','PROCESSING')",Integer.class,tx.get("id"));
        transition(tx,user,count>0?"REFUND_PENDING":"CANCELLED","CANCELLATION_APPROVED",reason);
        if(count==0){jdbc.update("update marketplace.transactions set cancelled_at=now() where id=?",tx.get("id"));release(tx);}
        jdbc.update("update marketplace_payment.payouts set status='CANCELLED' where transaction_id=? and status in ('ON_HOLD','READY')",tx.get("id"));
    }
    private void release(Map<String,Object> tx){jdbc.update("update marketplace.vehicle_listings set status=case when expires_at<=now() then 'EXPIRED' else 'PUBLISHED' end,updated_at=now(),version=version+1 where (id=? or id=?) and status='RESERVED'",tx.get("listing_id"),tx.get("exchange_listing_id"));}
    @Transactional
    public Map<String,Object> refund(String username,UUID id,UUID refund,String action,String reference){
        long user=actor(username);var tx=locked(id);own(tx,user);if(!"REFUND_PENDING".equals(tx.get("status")))throw conflict("Giao dịch không đang chờ hoàn tiền.");
        var r=one("select r.* from marketplace_payment.refunds r join marketplace_payment.payments p on p.id=r.payment_id where r.public_id=? and p.transaction_id=? for update of r",refund,tx.get("id"));
        if("sent".equals(action)){
            if(user!=payee(tx))throw forbidden();if(!"APPROVED".equals(r.get("status")))throw conflict("Khoản hoàn đã được xử lý.");
            String ref=reference(reference);jdbc.update("update marketplace_payment.refunds set status='PROCESSING',transfer_reference=?,sent_at=now() where id=?",ref,r.get("id"));
            event(tx.get("id"),user,"REFUND_PENDING","REFUND_PENDING","REFUND_REPORTED_SENT","Bên đã nhận tiền báo chuyển hoàn. Mã: "+ref);
        }else if("received".equals(action)){
            if(user!=payer(tx))throw forbidden();if(!"PROCESSING".equals(r.get("status")))throw conflict("Bên hoàn tiền chưa báo đã gửi hoặc khoản hoàn đã được xác nhận.");
            jdbc.update("update marketplace_payment.refunds set status='COMPLETED',received_at=now(),completed_at=now() where id=?",r.get("id"));
            jdbc.update("update marketplace_payment.payments set status='REFUNDED',updated_at=now() where id=?",r.get("payment_id"));
            int pending=jdbc.queryForObject("select count(*) from marketplace_payment.refunds r join marketplace_payment.payments p on p.id=r.payment_id where p.transaction_id=? and r.status<>'COMPLETED'",Integer.class,tx.get("id"));
            if(pending==0){transition(tx,user,"REFUNDED","REFUND_RECEIVED","Bên trả tiền xác nhận đã nhận đủ tiền hoàn.");jdbc.update("update marketplace.transactions set cancelled_at=now() where id=?",tx.get("id"));release(tx);}
            else event(tx.get("id"),user,"REFUND_PENDING","REFUND_PENDING","REFUND_RECEIVED","Đã nhận một khoản hoàn tiền.");
        }else throw bad("Thao tác không hợp lệ.");
        return detail(username,id);
    }
    @Transactional
    public Map<String,Object> dispute(String username,UUID id,String description,boolean staffAction){
        long user=actor(username);var tx=locked(id);if(staffAction)reviewer(tx,user);else own(tx,user);
        String status=(String)tx.get("status");if(TERMINAL.contains(status)||"DISPUTED".equals(status))throw conflict("Không thể mở tranh chấp ở trạng thái này.");
        String note=note(description);
        jdbc.update("insert into marketplace.disputes(transaction_id,opened_by,reason_code,description) values (?,?,'TRANSACTION_PROBLEM',?)",tx.get("id"),user,note);
        jdbc.update("update marketplace.transactions set dispute_previous_status=? where id=?",status,tx.get("id"));
        transition(tx,user,"DISPUTED","DISPUTE_OPENED",note);return detail(username,id);
    }
    @Transactional
    public Map<String,Object> resolveDispute(String username,UUID id,String decision,String resolution){
        long user=actor(username);var tx=locked(id);reviewer(tx,user);if(!"DISPUTED".equals(tx.get("status")))throw conflict("Giao dịch không đang tranh chấp.");
        String note=note(resolution);
        if("RESUME".equals(decision)){
            String previous=(String)tx.get("dispute_previous_status");
            if(previous==null||TERMINAL.contains(previous)||"DISPUTED".equals(previous))throw conflict("Không xác định được trạng thái trước tranh chấp.");
            transition(tx,user,previous,"DISPUTE_RESOLVED",note);
        }else if("CANCEL".equals(decision))finishCancellation(tx,user,note);else throw bad("Chọn tiếp tục giao dịch hoặc hủy và hoàn tiền.");
        jdbc.update("update marketplace.disputes set status='CLOSED',assigned_to=?,resolution=?,resolved_at=now(),updated_at=now() where transaction_id=? and status in ('OPEN','UNDER_REVIEW','WAITING_EVIDENCE')",user,decision+": "+note,tx.get("id"));
        jdbc.update("insert into ebike_admin.admin_audit_logs(actor,action,target,ip_address) values (?,'RESOLVE_TRADE_DISPUTE',?,'system')",username,id+" "+decision);
        return detail(username,id);
    }
    @Transactional
    public Map<String,Object> plan(String username,UUID id,OffsetDateTime time,String location,String description,int revision){
        long user=actor(username);var tx=locked(id);own(tx,user);active((String)tx.get("status"));checkRevision(tx,revision);
        if(time==null||time.isBefore(OffsetDateTime.now())||location==null||location.isBlank()||location.length()>500||description!=null&&description.length()>2000)throw bad("Nhập ngày giờ trong tương lai và địa điểm tối đa 500 ký tự.");
        if(tx.get("buyer_handover_confirmed_at")!=null||tx.get("seller_handover_confirmed_at")!=null)throw conflict("Không sửa lịch sau khi đã có xác nhận bàn giao.");
        boolean buyer=user==n(tx.get("buyer_id"));
        jdbc.update("update marketplace.transactions set handover_at=?,handover_location=?,handover_note=?,handover_revision=handover_revision+1,buyer_plan_agreed_at=case when ? then now() else null end,seller_plan_agreed_at=case when ? then null else now() end,updated_at=now(),version=version+1 where id=?",time,location.trim(),description,buyer,buyer,tx.get("id"));
        event(tx.get("id"),user,(String)tx.get("status"),(String)tx.get("status"),"HANDOVER_PROPOSED","Đề xuất lịch bàn giao mới; hai bên cần đồng ý cùng phiên bản.");
        return detail(username,id);
    }
    private void checkRevision(Map<String,Object> tx,int revision){if(n(tx.get("handover_revision"))!=revision)throw conflict("Lịch đã thay đổi. Hãy tải lại trước khi xác nhận.");}
    @Transactional
    public Map<String,Object> handover(String username,UUID id,String action,int revision){
        long user=actor(username);var tx=locked(id);own(tx,user);active((String)tx.get("status"));checkRevision(tx,revision);
        if(tx.get("handover_at")==null)throw conflict("Hai bên cần thống nhất lịch bàn giao.");
        boolean buyer=user==n(tx.get("buyer_id"));String column;
        if("agree".equals(action))column=buyer?"buyer_plan_agreed_at":"seller_plan_agreed_at";
        else if("confirm".equals(action)){
            if(!"FULLY_PAID".equals(tx.get("status"))||tx.get("buyer_plan_agreed_at")==null||tx.get("seller_plan_agreed_at")==null)throw conflict("Cần thanh toán đủ và hai bên đồng ý lịch trước khi xác nhận bàn giao.");
            if(jdbc.queryForObject("select count(*) from marketplace.transaction_evidence where transaction_id=?",Integer.class,tx.get("id"))==0)throw conflict("Cần bổ sung ít nhất một ảnh hoặc biên bản bàn giao.");
            column=buyer?"buyer_handover_confirmed_at":"seller_handover_confirmed_at";
        }else throw bad("Thao tác không hợp lệ.");
        if(tx.get(column)!=null)throw conflict("Bạn đã xác nhận bước này.");
        jdbc.update("update marketplace.transactions set "+column+"=now(),updated_at=now(),version=version+1 where id=?",tx.get("id"));
        event(tx.get("id"),user,(String)tx.get("status"),(String)tx.get("status"),"HANDOVER_"+action.toUpperCase(Locale.ROOT),buyer?"Người mua xác nhận.":"Người bán xác nhận.");
        var fresh=read(id);
        if(fresh.get("buyer_handover_confirmed_at")!=null&&fresh.get("seller_handover_confirmed_at")!=null){
            transition(fresh,user,"COMPLETED","BOTH_CONFIRMED_HANDOVER","Hai bên đã xác nhận bàn giao.");
            jdbc.update("update marketplace.transactions set completed_at=now() where id=?",tx.get("id"));
            jdbc.update("update marketplace.vehicle_listings set status=?,updated_at=now(),version=version+1 where id=? or id=?",tx.get("exchange_listing_id")==null?"SOLD":"EXCHANGED",tx.get("listing_id"),tx.get("exchange_listing_id"));
        }
        return detail(username,id);
    }
}
