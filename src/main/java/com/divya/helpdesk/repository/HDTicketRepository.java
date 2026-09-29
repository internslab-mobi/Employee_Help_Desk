package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDSlaPolicy;
import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.enums.TicketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDTicketRepository extends JpaRepository<HDTicket, Long>, JpaSpecificationExecutor<HDTicket> {

    // Employee - own tickets
    List<HDTicket> findByRequester_IdOrderByCreatedAtDesc(Long requesterId);

    // Employee - own tickets filtered by status
    List<HDTicket> findByRequester_IdAndStatusOrderByCreatedAtDesc(Long requesterId, TicketStatus status);

    // Agent - tickets assigned to agent
    List<HDTicket> findByAssignedAgent_IdOrderByCreatedAtDesc(Long agentId);

    // Agent - assigned tickets filtered by status
    List<HDTicket> findByAssignedAgent_IdAndStatusOrderByCreatedAtDesc(Long agentId, TicketStatus status);

    // Manager - tickets assigned to manager
    List<HDTicket> findByAssignedManager_IdOrderByCreatedAtDesc(Long managerId);

    // Manager - assigned to manager filtered by status
    List<HDTicket> findByAssignedManager_IdAndStatusOrderByCreatedAtDesc(Long managerId, TicketStatus status);

    // Manager - all tickets in their department
    List<HDTicket> findByDepartment_IdOrderByCreatedAtDesc(Long departmentId);

    // Manager - department tickets filtered by status
    List<HDTicket> findByDepartment_IdAndStatusOrderByCreatedAtDesc(Long departmentId, TicketStatus status);

    // Manager - department tickets filtered by agent
    List<HDTicket> findByDepartment_IdAndAssignedAgent_IdOrderByCreatedAtDesc(Long departmentId, Long agentId);

    // Manager - department + agent + status
    List<HDTicket> findByDepartment_IdAndAssignedAgent_IdAndStatusOrderByCreatedAtDesc(
            Long departmentId,
            Long agentId,
            TicketStatus status
    );

    // Used when calculating agent workload
    List<HDTicket> findByAssignedAgent_IdAndStatusIn(Long agentId, List<TicketStatus> statuses);


    // Used for checking duplicate/generated ticket numbers
    boolean existsByTicketNumber(String ticketNumber);

    // Used for checking last assigned ticket of an agent
    Optional<HDTicket> findTopByAssignedAgent_IdOrderByCreatedAtDesc(Long agentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM HDTicket t ORDER BY t.id DESC")
    List<HDTicket> findLastTicketForUpdate(Pageable pageable);
}