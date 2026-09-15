package com.ebike.marketplaceModule.service;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class OperationsPolicyTest {
 @Test void ownListingsCannotBeModeratedEvenByStaff() {
  assertEquals(403,assertThrows(ResponseStatusException.class,()->OperationsPolicy.transition("PENDING_REVIEW","APPROVE",true,null,null)).getStatusCode().value());
 }
 @Test void reviewLifecycleRequiresResubmission() {
  assertEquals("PUBLISHED",OperationsPolicy.transition("PENDING_REVIEW","APPROVE",false,null,null));
  assertEquals("REJECTED",OperationsPolicy.transition("PENDING_REVIEW","REQUEST_EDIT",false,"INVALID_IMAGE","Replace image"));
  assertThrows(ResponseStatusException.class,()->OperationsPolicy.transition("REJECTED","APPROVE",false,null,null));
  assertEquals("PENDING_REVIEW",OperationsPolicy.transition("SUSPENDED","RESTORE",false,"OTHER","Ready for another review"));
 }
 @Test void hideRequiresReasonAndCannotInterruptReservedSale() {
  assertThrows(ResponseStatusException.class,()->OperationsPolicy.transition("PUBLISHED","HIDE",false,"OTHER"," "));
  assertThrows(ResponseStatusException.class,()->OperationsPolicy.transition("PUBLISHED","HIDE",false,"INVALID","Reason"));
  assertThrows(ResponseStatusException.class,()->OperationsPolicy.transition("RESERVED","HIDE",false,"OTHER","Reason"));
  assertEquals("SUSPENDED",OperationsPolicy.transition("PUBLISHED","HIDE",false,"SUSPICIOUS_CONTENT","Reported fraud"));
 }
}
