package com.ebike.authModule.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountVerificationService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final AccountCodeDelivery delivery;
    private final SecureRandom random=new SecureRandom();
    public AccountVerificationService(JdbcTemplate jdbc, PasswordEncoder encoder, AccountCodeDelivery delivery) {
        this.jdbc=jdbc; this.encoder=encoder; this.delivery=delivery;
    }
    public Map<String,Boolean> availability() { return Map.of("emailAvailable",delivery.emailAvailable(),"phoneAvailable",delivery.phoneAvailable()); }

    @Transactional
    public Map<String,Object> request(String username, String email, String purpose) {
        boolean reset="RESET_PASSWORD".equals(purpose), phone="VERIFY_PHONE".equals(purpose);
        delivery.requireAvailable(phone);
        UUID id=UUID.randomUUID();
        List<Map<String,Object>> rows=reset
            ? jdbc.queryForList("select * from ebike_auth.users where lower(email)=? and is_active for update",email.trim().toLowerCase(Locale.ROOT))
            : jdbc.queryForList("select * from ebike_auth.users where username=? and is_active for update",username);
        Map<String,Object> response=Map.of("challengeId",id,"expiresInSeconds",600,"retryAfterSeconds",60,
            "message", reset ? "Nếu email thuộc tài khoản đang hoạt động, mã xác nhận sẽ được gửi tới hộp thư." : "Mã xác nhận đã được gửi.");
        if(rows.isEmpty()) {
            if(reset) return response;
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var user=rows.get(0);
        String target=(String)user.get(phone?"phone_number":"email");
        if(target==null || target.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Hãy lưu số điện thoại trong hồ sơ trước khi xác minh.");
        if(phone) target=AccountCodeDelivery.normalizePhone(target);
        Integer recent=jdbc.queryForObject("select count(*) from ebike_auth.account_challenges where user_id=? and purpose=? and created_at > now()-interval '1 minute'",Integer.class,user.get("id"),purpose);
        Integer hourly=jdbc.queryForObject("select count(*) from ebike_auth.account_challenges where user_id=? and created_at > now()-interval '1 hour'",Integer.class,user.get("id"));
        if(recent>0 || hourly>=10) {
            if(reset) return response;
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Vui lòng chờ trước khi yêu cầu mã mới.");
        }
        String code=String.format(Locale.ROOT,"%06d",random.nextInt(1000000));
        String reference=null;
        try {
            if(phone) reference=delivery.sendPhone(target); else delivery.sendEmail(target,code,reset);
        } catch(ResponseStatusException ex) {
            // A delivery failure must not reveal whether a password-reset email exists.
            if(reset) return response;
            throw ex;
        }
        jdbc.update("update ebike_auth.account_challenges set consumed_at=now() where user_id=? and purpose=? and consumed_at is null",user.get("id"),purpose);
        jdbc.update("insert into ebike_auth.account_challenges(public_id,user_id,purpose,target,code_hash,provider_reference) values (?,?,?,?,?,?)",id,user.get("id"),purpose,target,phone?null:encoder.encode(code),reference);
        return response;
    }

    // Invalid attempts must commit; rolling them back would allow unlimited guesses.
    @Transactional(noRollbackFor=InvalidCode.class)
    public void verify(String username, UUID id, String code, String purpose, String newPassword) {
        if("RESET_PASSWORD".equals(purpose)) validatePassword(newPassword);
        // Use the same lock order as request() to serialize resends, profile changes and consumption.
        var owners=jdbc.queryForList("select u.* from ebike_auth.users u join ebike_auth.account_challenges c on c.user_id=u.id where c.public_id=? for update of u",id);
        if(owners.isEmpty()) throw new InvalidCode();
        var user=owners.get(0);
        var rows=jdbc.queryForList("select *, expires_at>now() as valid from ebike_auth.account_challenges where public_id=? for update",id);
        var challenge=rows.get(0);
        boolean reset="RESET_PASSWORD".equals(purpose), phone="VERIFY_PHONE".equals(purpose);
        if(!purpose.equals(challenge.get("purpose")) || (!reset && !Objects.equals(username,user.get("username")))
            || !Boolean.TRUE.equals(user.get("is_active")) || challenge.get("consumed_at")!=null
            || !Boolean.TRUE.equals(challenge.get("valid")) || ((Number)challenge.get("attempts")).intValue()>=5) throw new InvalidCode();
        String currentTarget=(String)user.get(phone?"phone_number":"email");
        if(phone && currentTarget!=null) currentTarget=AccountCodeDelivery.normalizePhone(currentTarget);
        if(!Objects.equals(currentTarget,challenge.get("target"))) {
            jdbc.update("update ebike_auth.account_challenges set consumed_at=now() where public_id=?",id);
            throw new InvalidCode();
        }
        jdbc.update("update ebike_auth.account_challenges set attempts=attempts+1 where public_id=?",id);
        boolean accepted=phone ? delivery.checkPhone((String)challenge.get("provider_reference"),code) : encoder.matches(code,(String)challenge.get("code_hash"));
        if(!accepted) throw new InvalidCode();
        if(reset) jdbc.update("update ebike_auth.users set password_hash=?,token_version=token_version+1,account_version=account_version+1,updated_at=now() where id=?",encoder.encode(newPassword),user.get("id"));
        else if(phone) jdbc.update("update ebike_auth.users set phone_verified_at=now(),account_version=account_version+1,updated_at=now() where id=?",user.get("id"));
        else jdbc.update("update ebike_auth.users set email_verified_at=now(),is_verified=true,account_version=account_version+1,updated_at=now() where id=?",user.get("id"));
        jdbc.update("update ebike_auth.account_challenges set consumed_at=now() where user_id=? and purpose=? and consumed_at is null",user.get("id"),purpose);
    }

    static void validatePassword(String password) {
        if(password==null || password.length()<8 || password.getBytes(StandardCharsets.UTF_8).length>72 || password.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mật khẩu cần ít nhất 8 ký tự, tối đa 72 byte UTF-8.");
    }
    public static class InvalidCode extends ResponseStatusException {
        public InvalidCode() { super(HttpStatus.BAD_REQUEST,"Mã không hợp lệ, đã hết hạn hoặc đã sử dụng. Vui lòng yêu cầu mã mới nếu cần."); }
    }
}
