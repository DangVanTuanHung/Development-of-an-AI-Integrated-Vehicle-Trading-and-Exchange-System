package com.ebike.supportModule.repository;
import com.ebike.supportModule.entity.SupportTicket;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);
}
