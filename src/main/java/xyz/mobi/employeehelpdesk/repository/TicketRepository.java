package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.entity.Ticket;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TicketRepository
        extends JpaRepository<Ticket, Long> {

    // Count active tickets for a particular DepartmentAgent.id
    @Query("""
    SELECT COUNT(t)
    FROM Ticket t
    WHERE t.assignedAgent.id = :agentId
      AND t.status IN :statuses
    """)
    long countActiveTicketsForAgent(
            @Param("agentId") Long agentId,
            @Param("statuses") Collection<TicketStatus> statuses
    );

    /**
     * Batch count active tickets for multiple agents in one query.
     * Returns rows of [agentId, count].
     */
    @Query("""
    SELECT t.assignedAgent.id, COUNT(t)
    FROM Ticket t
    WHERE t.assignedAgent.id IN :agentIds
      AND t.status IN :statuses
    GROUP BY t.assignedAgent.id
    """)
    List<Object[]> countActiveTicketsForAgents(
            @Param("agentIds") Collection<Long> agentIds,
            @Param("statuses") Collection<TicketStatus> statuses
    );

    // --- Requester-based ticket queries ---

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager", "assignedManager.employee"})
    Page<Ticket> findByRequesterId(
            Long requesterId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager", "assignedManager.employee"})
    Page<Ticket> findByRequesterIdAndStatus(
            Long requesterId,
            TicketStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager", "assignedManager.employee"})
    Page<Ticket> findByRequesterIdAndCreatedAtBetween(
            Long requesterId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager", "assignedManager.employee"})
    Page<Ticket> findByRequesterIdAndStatusAndCreatedAtBetween(
            Long requesterId,
            TicketStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    @Query("""
    SELECT t
    FROM Ticket t
    LEFT JOIN FETCH t.requester
    LEFT JOIN FETCH t.department
    LEFT JOIN FETCH t.category
    LEFT JOIN FETCH t.subCategory
    LEFT JOIN FETCH t.assignedAgent aa
    LEFT JOIN FETCH aa.employee
    LEFT JOIN FETCH t.assignedManager am
    LEFT JOIN FETCH am.employee
    WHERE t.requester.id = :requesterId
      AND (
          LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
      )
    """)
    Page<Ticket> searchMyTickets(
            @Param("requesterId") Long requesterId,
            @Param("search") String search,
            Pageable pageable
    );

    // --- Agent-based ticket queries (using DepartmentAgent.id resolved from Employee) ---

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager", "assignedManager.employee"})
    Page<Ticket> findByAssignedAgentIdIn(
            Collection<Long> agentIds,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager", "assignedManager.employee"})
    Page<Ticket> findByAssignedAgentIdInAndStatus(
            Collection<Long> agentIds,
            TicketStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager", "assignedManager.employee"})
    Page<Ticket> findByAssignedAgentIdInAndCreatedAtBetween(
            Collection<Long> agentIds,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager", "assignedManager.employee"})
    Page<Ticket> findByAssignedAgentIdInAndStatusAndCreatedAtBetween(
            Collection<Long> agentIds,
            TicketStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    @Query("""
    SELECT t
    FROM Ticket t
    LEFT JOIN FETCH t.requester
    LEFT JOIN FETCH t.department
    LEFT JOIN FETCH t.category
    LEFT JOIN FETCH t.subCategory
    LEFT JOIN FETCH t.assignedAgent aa
    LEFT JOIN FETCH aa.employee
    LEFT JOIN FETCH t.assignedManager am
    LEFT JOIN FETCH am.employee
    WHERE t.assignedAgent.id IN :agentIds
      AND (
          LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
      )
    """)
    Page<Ticket> searchMyAssignedTickets(
            @Param("agentIds") Collection<Long> agentIds,
            @Param("search") String search,
            Pageable pageable
    );

    boolean existsByCategoryId(Long categoryId);

    boolean existsBySubCategoryId(Long subCategoryId);
}