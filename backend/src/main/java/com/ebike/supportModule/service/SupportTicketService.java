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
    private final SupportTicketRepository repository; private final UserRepository users;private final org.springframework.jdbc.core.JdbcTemplate db;
    public SupportTicketService(SupportTicketRepository repository, UserRepository users,org.springframework.jdbc.core.JdbcTemplate db){this.repository=repository;this.users=users;this.db=db;}

    public SupportTicketResponse create(SupportTicketCreateRequest request, Authentication auth){
        require(request.requesterName(), "Họ tên"); require(request.requesterEmail(), "Email"); require(request.subject(), "Chủ đề"); require(request.message(), "Nội dung");
        if (!request.requesterEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Email không hợp lệ");
        SupportTicket ticket=new SupportTicket(); ticket.setTicketCode("MX-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))+"-"+UUID.randomUUID().toString().substring(0,6).toUpperCase());
        ticket.setRequesterName(request.requesterName().trim()); ticket.setRequesterEmail(request.requesterEmail().trim().toLowerCase()); ticket.setCategory(clean(request.category(),"GENERAL").toUpperCase());
        ticket.setSubject(request.subject().trim()); ticket.setMessage(request.message().trim()); ticket.setStatus("OPEN"); ticket.setPriority("NORMAL");
        currentUser(auth).ifPresent(ticket::setUser);var saved=repository.saveAndFlush(ticket);
        db.queryForList("select marketplace.notify(ur.user_id,'Yêu cầu hỗ trợ mới',?,'/manager/support',false) from ebike_auth.user_roles ur join ebike_auth.roles r on r.id=ur.role_id where r.name in ('ADMIN','MANAGER')",saved.getTicketCode()+": "+saved.getSubject());
        return map(saved);
    }
    public List<SupportTicketResponse> mine(Authentication auth){ User user=currentUser(auth).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)); return repository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::map).toList(); }
    public List<SupportTicketResponse> all(String status, String search){
        return repository.findAllByOrderByCreatedAtDesc().stream().filter(t->status==null||status.isBlank()||t.getStatus().equalsIgnoreCase(status)).filter(t->search==null||search.isBlank()||(t.getTicketCode()+" "+t.getRequesterName()+" "+t.getRequesterEmail()+" "+t.getSubject()).toLowerCase().contains(search.toLowerCase())).map(this::map).toList();
    }
    public SupportTicketResponse update(Long id, SupportTicketUpdateRequest request, Authentication auth){
        SupportTicket t=repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy yêu cầu"));
        if(request.status()!=null){String value=request.status().toUpperCase();if(!STATUSES.contains(value))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trạng thái không hợp lệ");t.setStatus(value);t.setResolvedAt(Set.of("RESOLVED","CLOSED").contains(value)?LocalDateTime.now():null);}
        if(request.priority()!=null){String value=request.priority().toUpperCase();if(!PRIORITIES.contains(value))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mức ưu tiên không hợp lệ");t.setPriority(value);}
        boolean changed=request.staffNote()!=null&&!request.staffNote().trim().isBlank()&&!java.util.Objects.equals(request.staffNote().trim(),t.getStaffNote());
        if(request.staffNote()!=null){if(request.staffNote().length()>4000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ghi chú tối đa 4000 ký tự");t.setStaffNote(request.staffNote().trim());}t.setAssignedTo(request.assignedTo()!=null&&!request.assignedTo().isBlank()?request.assignedTo().trim():auth.getName());var saved=repository.saveAndFlush(t);
        if(changed){User author=currentUser(auth).orElseThrow();db.update("insert into ebike_support.ticket_messages(ticket_id,author_id,content) values (?,?,?)",saved.getId(),author.getId(),saved.getStaffNote());}
        return map(saved);
    }
    private java.util.Optional<User> currentUser(Authentication auth){if(auth==null||!auth.isAuthenticated())return java.util.Optional.empty();return users.findByUsername(auth.getName()).or(()->users.findByEmail(auth.getName()));}
    private void require(String v,String name){if(v==null||v.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,name+" là bắt buộc");}
    private String clean(String v,String fallback){return v==null||v.isBlank()?fallback:v.trim();}
    private SupportTicketResponse map(SupportTicket t){return new SupportTicketResponse(t.getId(),t.getTicketCode(),t.getRequesterName(),t.getRequesterEmail(),t.getCategory(),t.getSubject(),t.getMessage(),t.getStatus(),t.getPriority(),t.getStaffNote(),t.getAssignedTo(),t.getCreatedAt(),t.getUpdatedAt(),t.getResolvedAt());}
}
