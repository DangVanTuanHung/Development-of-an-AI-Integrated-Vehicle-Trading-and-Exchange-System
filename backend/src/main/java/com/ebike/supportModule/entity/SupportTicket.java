package com.ebike.supportModule.entity;

import com.ebike.authModule.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets", schema = "ebike_support")
public class SupportTicket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "ticket_code", nullable = false, unique = true, length = 24) private String ticketCode;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @Column(name = "requester_name", nullable = false, length = 160) private String requesterName;
    @Column(name = "requester_email", nullable = false) private String requesterEmail;
    @Column(nullable = false, length = 40) private String category;
    @Column(nullable = false, length = 220) private String subject;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
    @Column(nullable = false, length = 30) private String status;
    @Column(nullable = false, length = 20) private String priority;
    @Column(name = "staff_note", columnDefinition = "TEXT") private String staffNote;
    @Column(name = "assigned_to", length = 120) private String assignedTo;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
    @PrePersist void create() { var now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
    public Long getId(){return id;} public String getTicketCode(){return ticketCode;} public void setTicketCode(String v){ticketCode=v;}
    public User getUser(){return user;} public void setUser(User v){user=v;} public String getRequesterName(){return requesterName;} public void setRequesterName(String v){requesterName=v;}
    public String getRequesterEmail(){return requesterEmail;} public void setRequesterEmail(String v){requesterEmail=v;} public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public String getSubject(){return subject;} public void setSubject(String v){subject=v;} public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getPriority(){return priority;} public void setPriority(String v){priority=v;}
    public String getStaffNote(){return staffNote;} public void setStaffNote(String v){staffNote=v;} public String getAssignedTo(){return assignedTo;} public void setAssignedTo(String v){assignedTo=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;} public LocalDateTime getResolvedAt(){return resolvedAt;} public void setResolvedAt(LocalDateTime v){resolvedAt=v;}
}
