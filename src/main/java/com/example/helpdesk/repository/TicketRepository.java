package com.example.helpdesk.repository;

import com.example.helpdesk.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsByTicketNumber(String ticketNumber);

    List<Ticket> findByRequesterId(Long requesterId);

    List<Ticket> findByDepartmentId(Long departmentId);

    List<Ticket> findByAssignedAgentId(Long agentId);

    List<Ticket> findByStatus(String status);

    long countByAssignedAgentIdAndStatusNotIn(Long agentId, List<String> statuses);
}