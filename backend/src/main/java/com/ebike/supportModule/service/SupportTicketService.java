package com.ebike.supportModule.service;

import com.ebike.authModule.entity.User;
import com.ebike.authModule.repository.UserRepository;
import com.ebike.supportModule.dto.*;
import com.ebike.supportModule.entity.SupportTicket;
import com.ebike.supportModule.repository.SupportTicketRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupportTicketService {
    private static final Set<String> STATUSES = Set.of("OPEN", "IN_PROGRESS", "WAITING_CUSTOMER", "RESOLVED", "CLOSED");
    private static final Set<String> PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private final SupportTicketRepository repository; private final UserRepository users;
    public SupportTicketService(SupportTicketRepository repository, UserRepository users){this.repository=repository;this.users=users;}

    public SupportTicketResponse create(SupportTicketCreateRequest request, Authentication auth){
        require(request.requesterName(), "Họ tên"); require(request.requesterEmail(), "Email"); require(request.subject(), "Chủ đề"); require(request.message(), "Nội dung");
        if (!request.requesterEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Email không hợp lệ");
        SupportTicket ticket=new SupportTicket(); ticket.setTicketCode("MX-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))+"-"+UUID.randomUUID().toString().substring(0,6).toUpperCase());
        ticket.setRequesterName(request.requesterName().trim()); ticket.setRequesterEmail(request.requesterEmail().trim().toLowerCase()); ticket.setCategory(clean(request.category(),"GENERAL").toUpperCase());
        ticket.setSubject(request.subject().trim()); ticket.setMessage(request.message().trim()); ticket.setStatus("OPEN"); ticket.setPriority("NORMAL");
        currentUser(auth).ifPresent(ticket::setUser); return map(repository.save(ticket));
    }
    public List<SupportTicketResponse> mine(Authentication auth){ User user=currentUser(auth).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)); return repository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::map).toList(); }
    public List<SupportTicketResponse> all(String status, String search){
        return repository.findAllByOrderByCreatedAtDesc().stream().filter(t->status==null||status.isBlank()||t.getStatus().equalsIgnoreCase(status)).filter(t->search==null||search.isBlank()||(t.getTicketCode()+" "+t.getRequesterName()+" "+t.getRequesterEmail()+" "+t.getSubject()).toLowerCase().contains(search.toLowerCase())).map(this::map).toList();
    }
    public SupportTicketResponse update(Long id, SupportTicketUpdateRequest request, Authentication auth){
        SupportTicket t=repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy yêu cầu"));
        if(request.status()!=null){String value=request.status().toUpperCase();if(!STATUSES.contains(value))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trạng thái không hợp lệ");t.setStatus(value);t.setResolvedAt(Set.of("RESOLVED","CLOSED").contains(value)?LocalDateTime.now():null);}
        if(request.priority()!=null){String value=request.priority().toUpperCase();if(!PRIORITIES.contains(value))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mức ưu tiên không hợp lệ");t.setPriority(value);}
        if(request.staffNote()!=null)t.setStaffNote(request.staffNote().trim()); t.setAssignedTo(request.assignedTo()!=null&&!request.assignedTo().isBlank()?request.assignedTo().trim():auth.getName()); return map(repository.save(t));
    }
    private java.util.Optional<User> currentUser(Authentication auth){if(auth==null||!auth.isAuthenticated())return java.util.Optional.empty();return users.findByUsername(auth.getName()).or(()->users.findByEmail(auth.getName()));}
    private void require(String v,String name){if(v==null||v.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,name+" là bắt buộc");}
    private String clean(String v,String fallback){return v==null||v.isBlank()?fallback:v.trim();}
    private SupportTicketResponse map(SupportTicket t){return new SupportTicketResponse(t.getId(),t.getTicketCode(),t.getRequesterName(),t.getRequesterEmail(),t.getCategory(),t.getSubject(),t.getMessage(),t.getStatus(),t.getPriority(),t.getStaffNote(),t.getAssignedTo(),t.getCreatedAt(),t.getUpdatedAt(),t.getResolvedAt());}
}
