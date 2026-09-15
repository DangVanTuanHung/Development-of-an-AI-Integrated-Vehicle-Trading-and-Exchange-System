package com.ebike.supportModule.dto;
import java.time.LocalDateTime;
public record SupportTicketResponse(Long id, String ticketCode, String requesterName, String requesterEmail, String category, String subject, String message, String status, String priority, String staffNote, String assignedTo, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime resolvedAt) {}
