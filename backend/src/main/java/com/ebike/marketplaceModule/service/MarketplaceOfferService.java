package com.ebike.marketplaceModule.service;

import static com.ebike.marketplaceModule.service.TradePolicy.*;
import com.ebike.marketplaceModule.dto.CreateOfferRequest;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MarketplaceOfferService {
    private final JdbcTemplate jdbc;
    private final MarketplaceTransactionService trades;
    public MarketplaceOfferService(JdbcTemplate jdbc,MarketplaceTransactionService trades){this.jdbc=jdbc;this.trades=trades;}
    private static long n(Object value){return ((Number)value).longValue();}
    private Map<String,Object> one(String sql,Object... args){var rows=jdbc.queryForList(sql,args);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);return rows.get(0);}
    private long actor(String username){return n(one("select id from ebike_auth.users where username=? and is_active",username).get("id"));}
    private void lockVehicles(Object main,Object exchange){jdbc.queryForList("select id from marketplace.vehicle_listings where id=? or id=? order by id for update",main,exchange);}
    private BigDecimal exchange(CreateOfferRequest request){return request.exchangeValue()==null?BigDecimal.ZERO:request.exchangeValue();}
    private void agreement(CreateOfferRequest request){if(!request.policyAccepted())throw bad("Cần đồng ý chính sách hoàn tiền khi hai bên thống nhất hủy giao dịch.");terms(request.amount(),exchange(request),request.depositAmount());}
    private void validateVehicles(Map<String,Object> listing,Object exchangeId,long buyer,BigDecimal value){
        if(!"PUBLISHED".equals(listing.get("status")))throw conflict("Tin không còn nhận đề nghị.");
        if(exchangeId==null){if("EXCHANGE".equals(listing.get("listing_type")))throw bad("Tin này chỉ nhận trao đổi; cần chọn xe của bạn.");if(value.signum()!=0)throw bad("Chọn xe trao đổi trước khi định giá xe đó.");return;}
        if(!Boolean.TRUE.equals(listing.get("exchange_allowed"))&&"SALE".equals(listing.get("listing_type")))throw bad("Tin này không chấp nhận trao đổi.");
        var car=one("select * from marketplace.vehicle_listings where id=?",exchangeId);
        if(n(car.get("seller_id"))!=buyer||!"PUBLISHED".equals(car.get("status"))||n(car.get("id"))==n(listing.get("id"))||value.signum()<=0)
            throw bad("Xe trao đổi phải là tin đang đăng của người mua và có giá trị thỏa thuận lớn hơn 0.");
    }
    @Transactional
    public Map<String,Object> create(String username,UUID listingId,CreateOfferRequest request){
        agreement(request);long buyer=actor(username);
        var listing=one("select * from marketplace.vehicle_listings where public_id=?",listingId);
        Object exchangeId=request.exchangeListingId()==null?null:one("select id from marketplace.vehicle_listings where public_id=?",request.exchangeListingId()).get("id");
        lockVehicles(listing.get("id"),exchangeId);listing=one("select * from marketplace.vehicle_listings where id=?",listing.get("id"));
        if(n(listing.get("seller_id"))==buyer)throw bad("Không thể gửi đề nghị cho tin của chính mình.");
        validateVehicles(listing,exchangeId,buyer,exchange(request));
        expire();
        if(Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from marketplace.offers where listing_id=? and buyer_id=? and status='PENDING')",Boolean.class,listing.get("id"),buyer)))throw conflict("Đã có đề nghị đang chờ; hãy thương lượng hoặc rút đề nghị đó.");
        Long id=jdbc.queryForObject("insert into marketplace.offers(public_id,listing_id,buyer_id,seller_id,proposed_by,amount,deposit_amount,message,expires_at,exchange_listing_id,exchange_value) values (?,?,?,?,?,?,?,?,now()+interval '3 days',?,?) returning id",Long.class,UUID.randomUUID(),listing.get("id"),buyer,listing.get("seller_id"),buyer,request.amount(),request.depositAmount(),request.message(),exchangeId,exchange(request));
        return row(id);
    }
    @Transactional
    public Map<String,Object> respond(String username,UUID id,String action,CreateOfferRequest request,boolean policyAccepted){
        long user=actor(username);var preview=one("select * from marketplace.offers where public_id=?",id);
        lockVehicles(preview.get("listing_id"),preview.get("exchange_listing_id"));
        var offer=one("select *,expires_at>now() as valid from marketplace.offers where public_id=? for update",id);
        if(user!=n(offer.get("buyer_id"))&&user!=n(offer.get("seller_id")))throw forbidden();
        if(!"PENDING".equals(offer.get("status"))||!Boolean.TRUE.equals(offer.get("valid")))throw conflict("Đề nghị đã được phản hồi hoặc hết hạn.");
        boolean proposer=user==n(offer.get("proposed_by"));
        if("withdraw".equals(action)){
            if(!proposer)throw forbidden();jdbc.update("update marketplace.offers set status='WITHDRAWN',responded_at=now() where id=?",offer.get("id"));return row(n(offer.get("id")));
        }
        if(proposer)throw forbidden();
        if("reject".equals(action)){
            jdbc.update("update marketplace.offers set status='REJECTED',responded_at=now() where id=?",offer.get("id"));return row(n(offer.get("id")));
        }
        var listing=one("select * from marketplace.vehicle_listings where id=?",offer.get("listing_id"));
        validateVehicles(listing,offer.get("exchange_listing_id"),n(offer.get("buyer_id")),(BigDecimal)offer.get("exchange_value"));
        if("counter".equals(action)){
            agreement(request);
            if(offer.get("exchange_listing_id")==null&&exchange(request).signum()!=0)throw bad("Đề nghị mua không có xe trao đổi.");
            if(offer.get("exchange_listing_id")!=null&&exchange(request).signum()<=0)throw bad("Giá trị xe trao đổi phải lớn hơn 0.");
            jdbc.update("update marketplace.offers set status='COUNTERED',responded_at=now() where id=?",offer.get("id"));
            Long child=jdbc.queryForObject("insert into marketplace.offers(public_id,listing_id,buyer_id,seller_id,proposed_by,amount,deposit_amount,message,expires_at,parent_offer_id,exchange_listing_id,exchange_value) values (?,?,?,?,?,?,?,?,now()+interval '3 days',?,?,?) returning id",Long.class,UUID.randomUUID(),offer.get("listing_id"),offer.get("buyer_id"),offer.get("seller_id"),user,request.amount(),request.depositAmount(),request.message(),offer.get("id"),offer.get("exchange_listing_id"),exchange(request));
            return row(child);
        }
        if(!"accept".equals(action))throw bad("Thao tác không hợp lệ.");
        if(!policyAccepted)throw bad("Cần đồng ý chính sách hủy và hoàn tiền.");
        jdbc.update("update marketplace.offers set status='ACCEPTED',responded_at=now() where id=?",offer.get("id"));
        jdbc.update("update marketplace.offers set status='REJECTED',responded_at=now() where id<>? and status='PENDING' and (listing_id in (?,?) or exchange_listing_id in (?,?))",offer.get("id"),offer.get("listing_id"),offer.get("exchange_listing_id"),offer.get("listing_id"),offer.get("exchange_listing_id"));
        jdbc.update("update marketplace.vehicle_listings set status='RESERVED',updated_at=now(),version=version+1 where id=? or id=?",offer.get("listing_id"),offer.get("exchange_listing_id"));
        BigDecimal payable=((BigDecimal)offer.get("amount")).subtract((BigDecimal)offer.get("exchange_value")).abs();
        UUID publicId=UUID.randomUUID();String status=initial(payable,(BigDecimal)offer.get("deposit_amount"));
        Long tx=jdbc.queryForObject("insert into marketplace.transactions(public_id,transaction_number,listing_id,accepted_offer_id,buyer_id,seller_id,agreed_price,deposit_amount,exchange_listing_id,exchange_value,status) values (?,?,?,?,?,?,?,?,?,?,?) returning id",Long.class,publicId,"TX-"+UUID.randomUUID().toString().substring(0,18).toUpperCase(Locale.ROOT),offer.get("listing_id"),offer.get("id"),offer.get("buyer_id"),offer.get("seller_id"),offer.get("amount"),offer.get("deposit_amount"),offer.get("exchange_listing_id"),offer.get("exchange_value"),status);
        trades.event(tx,user,"CREATED",status,"OFFER_ACCEPTED","Hai bên đồng ý đề nghị và chính sách hoàn tiền khi thống nhất hủy.");
        return trades.detail(username,publicId);
    }
    @Transactional
    public List<Map<String,Object>> mine(String username){long user=actor(username);expire();return jdbc.queryForList(query()+" where o.buyer_id=? or o.seller_id=? order by o.created_at desc",user,user);}
    private Map<String,Object> row(long id){return one(query()+" where o.id=?",id);}
    private String query(){return """
        select o.public_id as "publicId",l.public_id as "listingPublicId",l.title as "listingTitle",o.buyer_id as "buyerId",o.seller_id as "sellerId",
        b.username as "buyerName",s.username as "sellerName",o.proposed_by as "proposedBy",o.amount,o.deposit_amount as "depositAmount",o.exchange_value as "exchangeValue",
        e.public_id as "exchangeListingPublicId",e.title as "exchangeListingTitle",o.message,o.status,o.expires_at as "expiresAt",o.created_at as "createdAt",o.parent_offer_id as "parentOfferId"
        from marketplace.offers o join marketplace.vehicle_listings l on l.id=o.listing_id
        join ebike_auth.users b on b.id=o.buyer_id join ebike_auth.users s on s.id=o.seller_id
        left join marketplace.vehicle_listings e on e.id=o.exchange_listing_id
        """;}
    @Scheduled(fixedDelay=60000) @Transactional
    public void expire(){jdbc.update("update marketplace.offers set status='EXPIRED',responded_at=now() where status='PENDING' and expires_at<=now()");}
}
