package com.ebike.marketplaceModule.service;

import java.math.BigDecimal;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class TradePolicy {
    private TradePolicy() {}
    public static final Set<String> TERMINAL=Set.of("COMPLETED","CANCELLED","REFUNDED");
    public static final Set<String> FROZEN=Set.of("DISPUTED","CANCELLATION_REQUESTED","REFUND_PENDING");
    public static void terms(BigDecimal price,BigDecimal exchange,BigDecimal deposit) {
        if(price==null||exchange==null||deposit==null||price.signum()<=0||exchange.signum()<0||deposit.signum()<0
            || deposit.compareTo(price.subtract(exchange).abs())>0 || price.scale()>2||exchange.scale()>2||deposit.scale()>2
            ||price.precision()>18||exchange.precision()>18||deposit.precision()>18)
            throw bad("Giá trị xe phải dương; tiền cọc từ 0 đến số tiền cần bù/thanh toán. Số tiền tối đa hai chữ số thập phân.");
    }
    public static String initial(BigDecimal payable,BigDecimal deposit) {
        return payable.signum()==0?"FULLY_PAID":deposit.signum()==0?"FINAL_PAYMENT_PENDING":"DEPOSIT_PENDING";
    }
    public static void active(String status) {
        if(TERMINAL.contains(status)||FROZEN.contains(status)) throw conflict("Giao dịch đang bị tạm dừng hoặc đã kết thúc.");
    }
    public static ResponseStatusException bad(String message) {return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    public static ResponseStatusException conflict(String message) {return new ResponseStatusException(HttpStatus.CONFLICT,message);}
    public static ResponseStatusException forbidden() {return new ResponseStatusException(HttpStatus.FORBIDDEN,"Bạn không có quyền thực hiện thao tác này.");}
    public static String note(String value) {
        if(value==null||value.isBlank()||value.length()>2000)throw bad("Nhập nội dung từ 1 đến 2000 ký tự.");
        return value.trim();
    }
}
