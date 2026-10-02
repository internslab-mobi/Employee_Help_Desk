package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDTicketEntity;
import com.divya.helpdesk.enums.TicketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDTicketRepository extends JpaRepository<HDTicketEntity, Long>, JpaSpecificationExecutor<HDTicketEntity> {

    // Employee - own tickets
    List<HDTicketEntity> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    // Employee - own tickets filtered by status
    List<HDTicketEntity> findByRequesterIdAndStatusOrderByCreatedAtDesc(Long requesterId, TicketStatus status);

    // Agent - tickets assigned to agent
    List<HDTicketEntity> findByAssignedAgentIdOrderByCreatedAtDesc(Long agentId);

    // Agent - assigned tickets filtered by status
    List<HDTicketEntity> findByAssignedAgentIdAndStatusOrderByCreatedAtDesc(Long agentId, TicketStatus status);

    // Manager - tickets assigned to manager
    List<HDTicketEntity> findByAssignedManagerIdOrderByCreatedAtDesc(Long managerId);

    // Manager - assigned to manager filtered by status
    List<HDTicketEntity> findByAssignedManagerIdAndStatusOrderByCreatedAtDesc(Long managerId, TicketStatus status);

    // Used when calculating agent workload
    List<HDTicketEntity> findByAssignedAgentIdAndStatusIn(Long agentId, List<TicketStatus> statuses);

    // Used for checking duplicate/generated ticket numbers
    boolean existsByTicketNumber(String ticketNumber);

    // Used for checking last assigned ticket of an agent
    Optional<HDTicketEntity> findTopByAssignedAgentIdOrderByCreatedAtDesc(Long agentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM HDTicketEntity t ORDER BY t.id DESC")
    List<HDTicketEntity> findLastTicketForUpdate(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM HDTicketEntity t WHERE t.id = :id")
    Optional<HDTicketEntity> findByIdForUpdate(@Param("id") Long id);

    Page<HDTicketEntity> findByDepartmentIdAndAssignedAgentIdAndStatus(Long departmentId, Long agentId, TicketStatus status, Pageable pageable);

    Page<HDTicketEntity> findByDepartmentIdAndAssignedAgentId(Long departmentId, Long agentId, Pageable pageable);

    Page<HDTicketEntity> findByDepartmentIdAndStatus(Long departmentId, TicketStatus status, Pageable pageable);

    Page<HDTicketEntity> findByDepartmentId(Long departmentId, Pageable pageable);
}