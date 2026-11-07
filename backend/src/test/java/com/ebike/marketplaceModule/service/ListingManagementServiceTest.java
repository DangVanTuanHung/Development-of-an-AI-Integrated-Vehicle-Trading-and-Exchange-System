package com.ebike.marketplaceModule.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ListingManagementServiceTest {
    @Test void soldAndReservedListingsCannotBeReopenedOrHidden() {
        for(String status : new String[]{"SOLD","RESERVED","SUSPENDED"})
            for(String action : new String[]{"hide","relist","sold","renew"})
                assertThrows(ResponseStatusException.class,()->ListingManagementService.nextStatus(status,action));
    }
    @Test void relistingRequiresAnotherReviewAndExpiryCannotPublishDirectly() {
        assertEquals("DRAFT",ListingManagementService.nextStatus("ARCHIVED","relist"));
        assertEquals("DRAFT",ListingManagementService.nextStatus("HIDDEN","relist"));
        assertEquals("PENDING_REVIEW",ListingManagementService.nextStatus("EXPIRED","renew"));
        assertThrows(ResponseStatusException.class,()->ListingManagementService.nextStatus("REJECTED","renew"));
    }
    @Test void onlyPublishedOrPendingListingsCanBeTemporarilyHidden() {
        assertEquals("HIDDEN",ListingManagementService.nextStatus("PUBLISHED","hide"));
        assertEquals("HIDDEN",ListingManagementService.nextStatus("PENDING_REVIEW","hide"));
        assertThrows(ResponseStatusException.class,()->ListingManagementService.nextStatus("DRAFT","hide"));
    }
}
