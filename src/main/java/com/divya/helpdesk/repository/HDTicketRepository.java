package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.enums.HDTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDTicketRepository extends JpaRepository<HDTicket, Long>, JpaSpecificationExecutor<HDTicket> {
    Optional<HDTicket> findByTicketNumber(String ticketNumber);

    @Query("SELECT t FROM HDTicket t WHERE t.assignedAgent.id = :agentId AND t.status IN (com.divya.helpdesk.enums.HDTicketStatus.ASSIGNED, com.divya.helpdesk.enums.HDTicketStatus.IN_PROGRESS)")
    List<HDTicket> findActiveTicketsByAgentEmployeeId(@Param("agentId") Long agentId);
}
