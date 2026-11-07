package com.ebike.authModule.service;

import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountCodeDelivery {
    private final JavaMailSender mail;
    private final String mailHost, from, accountSid, authToken, serviceSid;
    private final RestTemplate http;

    public AccountCodeDelivery(JavaMailSender mail, RestTemplateBuilder builder,
        @Value("${spring.mail.host:}") String mailHost,
        @Value("${app.auth.verification.from:no-reply@motionx.local}") String from,
        @Value("${app.auth.verification.twilio.account-sid:}") String accountSid,
        @Value("${app.auth.verification.twilio.auth-token:}") String authToken,
        @Value("${app.auth.verification.twilio.service-sid:}") String serviceSid) {
        this.mail=mail; this.mailHost=mailHost; this.from=from;
        this.accountSid=accountSid; this.authToken=authToken; this.serviceSid=serviceSid;
        http=builder.setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(10)).build();
    }

    public boolean emailAvailable() { return !mailHost.isBlank(); }
    public boolean phoneAvailable() { return accountSid.matches("AC[0-9a-fA-F]{32}") && !authToken.isBlank() && serviceSid.matches("VA[0-9a-fA-F]{32}"); }

    public void requireAvailable(boolean phone) {
        if (!(phone ? phoneAvailable() : emailAvailable())) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            phone ? "Dịch vụ xác minh SMS chưa được cấu hình." : "Dịch vụ gửi email chưa được cấu hình.");
    }

    public void sendEmail(String target, String code, boolean passwordReset) {
        requireAvailable(false);
        SimpleMailMessage message=new SimpleMailMessage();
        message.setFrom(from); message.setTo(target);
        message.setSubject(passwordReset ? "MOTIONX - Đặt lại mật khẩu" : "MOTIONX - Xác minh email");
        message.setText("Mã xác nhận của bạn: " + code + "\nMã có hiệu lực 10 phút và chỉ sử dụng một lần. Không chia sẻ mã này.\nNếu không yêu cầu, bạn có thể bỏ qua email này.");
        try { mail.send(message); }
        catch (org.springframework.mail.MailException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Chưa gửi được email. Vui lòng thử lại sau."); }
    }

    public String sendPhone(String phone) {
        requireAvailable(true);
        var body=new LinkedMultiValueMap<String,String>(); body.add("To", normalizePhone(phone)); body.add("Channel","sms");
        Map<?,?> response=post("Verifications",body);
        Object sid=response.get("sid");
        if (!(sid instanceof String value) || !value.matches("VE[0-9a-fA-F]{32}")) throw unavailable();
        return value;
    }

    public boolean checkPhone(String reference, String code) {
        requireAvailable(true);
        var body=new LinkedMultiValueMap<String,String>(); body.add("VerificationSid",reference); body.add("Code",code);
        try { return "approved".equals(post("VerificationCheck",body).get("status")); }
        catch (HttpClientErrorException.NotFound ex) { return false; }
    }

    private Map<?,?> post(String resource, LinkedMultiValueMap<String,String> body) {
        HttpHeaders headers=new HttpHeaders(); headers.setBasicAuth(accountSid,authToken); headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            Map<?,?> result=http.postForObject("https://verify.twilio.com/v2/Services/"+serviceSid+"/"+resource,new HttpEntity<>(body,headers),Map.class);
            if (result==null) throw unavailable();
            return result;
        } catch (HttpClientErrorException.NotFound ex) { throw ex; }
        catch (RestClientException ex) { throw unavailable(); }
    }
    public static String normalizePhone(String phone) {
        String value=phone==null ? "" : phone.replaceAll("[\\s().-]","");
        if(value.startsWith("0")) value="+84"+value.substring(1);
        if(!value.matches("\\+84[0-9]{9,10}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Số điện thoại Việt Nam không hợp lệ.");
        return value;
    }
    private ResponseStatusException unavailable() { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Dịch vụ SMS chưa thể xử lý. Vui lòng thử lại sau."); }
}
