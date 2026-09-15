package com.ebike.supportModule.dto;
public record SupportTicketUpdateRequest(String status, String priority, String staffNote, String assignedTo) {}
