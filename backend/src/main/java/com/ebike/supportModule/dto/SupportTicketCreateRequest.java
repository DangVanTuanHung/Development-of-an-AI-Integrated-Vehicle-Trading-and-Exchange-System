package com.ebike.supportModule.dto;
public record SupportTicketCreateRequest(String requesterName, String requesterEmail, String category, String subject, String message) {}
