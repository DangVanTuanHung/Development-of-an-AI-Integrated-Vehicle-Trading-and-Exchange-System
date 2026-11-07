package com.ebike.marketplaceModule.service;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
class TradePolicyTest {
    private BigDecimal m(String value){return new BigDecimal(value);}
    @Test void depositIsBoundToActualCashDifference(){
        assertDoesNotThrow(()->TradePolicy.terms(m("100"),m("120"),m("20")));
        assertThrows(ResponseStatusException.class,()->TradePolicy.terms(m("100"),m("120"),m("21")));
        assertThrows(ResponseStatusException.class,()->TradePolicy.terms(m("100"),m("100"),m("1")));
        assertThrows(ResponseStatusException.class,()->TradePolicy.terms(m("0"),m("0"),m("0")));
    }
    @Test void equalExchangeNeedsNoPaymentButStillRequiresHandover(){
        assertEquals("FULLY_PAID",TradePolicy.initial(m("0"),m("0")));
        assertEquals("FINAL_PAYMENT_PENDING",TradePolicy.initial(m("100"),m("0")));
        assertEquals("DEPOSIT_PENDING",TradePolicy.initial(m("100"),m("10")));
    }
    @Test void frozenAndEndedStatesRejectOrdinaryMutation(){
        for(String status:new String[]{"COMPLETED","CANCELLED","REFUNDED","DISPUTED","CANCELLATION_REQUESTED","REFUND_PENDING"})assertThrows(ResponseStatusException.class,()->TradePolicy.active(status));
        assertDoesNotThrow(()->TradePolicy.active("FULLY_PAID"));
    }
}
