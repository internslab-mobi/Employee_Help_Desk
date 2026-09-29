package xyz.mobi.employeehelpdesk.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository
        extends JpaRepository<Ticket, Long> {

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee"})
    Optional<Ticket> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee"})
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdWithLock(@Param("id") Long id);

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

    // --- GET ALL Queries ---

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee"})
    Page<Ticket> findByRequesterId(
            Long requesterId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee"})
    Page<Ticket> findByAssignedAgentId(
            Long agentId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee"})
    Page<Ticket> findByDepartmentId(
            Long departmentId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee"})
    @Query("SELECT t FROM Ticket t")
    Page<Ticket> findAllTickets(
            Pageable pageable
    );

    // --- SEARCH Queries with combined filters ---

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee"})
    @Query(value = """
    SELECT t
    FROM Ticket t
    WHERE t.requester.id = :requesterId
      AND (:status IS NULL OR t.status = :status)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND (
          :search IS NULL OR :search = ''
          OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
      )
      AND (
          :unassigned IS NULL
          OR (:unassigned = true AND t.assignedAgent IS NULL)
          OR (:unassigned = false AND t.assignedAgent IS NOT NULL)
      )
      AND (
          :slaStatus IS NULL
          OR EXISTS (
              SELECT s
              FROM SlaInstance s
              WHERE s.ticket.id = t.id
                AND s.status = :slaStatus
                AND s.cycleNumber = (
                    SELECT MAX(s2.cycleNumber)
                    FROM SlaInstance s2
                    WHERE s2.ticket.id = t.id
                )
          )
      )
    """, countQuery = """
    SELECT COUNT(t)
    FROM Ticket t
    WHERE t.requester.id = :requesterId
      AND (:status IS NULL OR t.status = :status)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND (
          :search IS NULL OR :search = ''
          OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
      )
      AND (
          :unassigned IS NULL
          OR (:unassigned = true AND t.assignedAgent IS NULL)
          OR (:unassigned = false AND t.assignedAgent IS NOT NULL)
      )
      AND (
          :slaStatus IS NULL
          OR EXISTS (
              SELECT s
              FROM SlaInstance s
              WHERE s.ticket.id = t.id
                AND s.status = :slaStatus
                AND s.cycleNumber = (
                    SELECT MAX(s2.cycleNumber)
                    FROM SlaInstance s2
                    WHERE s2.ticket.id = t.id
                )
          )
      )
    """)
    Page<Ticket> findCreatedTicketsWithFilters(
            @Param("requesterId") Long requesterId,
            @Param("status") TicketStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("search") String search,
            @Param("unassigned") Boolean unassigned,
            @Param("slaStatus") SlaStatus slaStatus,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee"})
    @Query(value = """
    SELECT t
    FROM Ticket t
    WHERE t.assignedAgent.id = :agentId
      AND (:status IS NULL OR t.status = :status)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND (
          :search IS NULL OR :search = ''
          OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
      )
      AND (
          :unassigned IS NULL
          OR (:unassigned = true AND t.assignedAgent IS NULL)
          OR (:unassigned = false AND t.assignedAgent IS NOT NULL)
      )
      AND (
          :slaStatus IS NULL
          OR EXISTS (
              SELECT s
              FROM SlaInstance s
              WHERE s.ticket.id = t.id
                AND s.status = :slaStatus
                AND s.cycleNumber = (
                    SELECT MAX(s2.cycleNumber)
                    FROM SlaInstance s2
                    WHERE s2.ticket.id = t.id
                )
          )
      )
    """, countQuery = """
    SELECT COUNT(t)
    FROM Ticket t
    WHERE t.assignedAgent.id = :agentId
      AND (:status IS NULL OR t.status = :status)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND (
          :search IS NULL OR :search = ''
          OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
      )
      AND (
          :unassigned IS NULL
          OR (:unassigned = true AND t.assignedAgent IS NULL)
          OR (:unassigned = false AND t.assignedAgent IS NOT NULL)
      )
      AND (
          :slaStatus IS NULL
          OR EXISTS (
              SELECT s
              FROM SlaInstance s
              WHERE s.ticket.id = t.id
                AND s.status = :slaStatus
                AND s.cycleNumber = (
                    SELECT MAX(s2.cycleNumber)
                    FROM SlaInstance s2
                    WHERE s2.ticket.id = t.id
                )
          )
      )
    """)
    Page<Ticket> findAssignedTicketsWithFilters(
            @Param("agentId") Long agentId,
            @Param("status") TicketStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("search") String search,
            @Param("unassigned") Boolean unassigned,
            @Param("slaStatus") SlaStatus slaStatus,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee"})
    @Query(value = """
    SELECT t
    FROM Ticket t
    WHERE (:departmentId IS NULL OR t.department.id = :departmentId)
      AND (:status IS NULL OR t.status = :status)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND (
          :search IS NULL OR :search = ''
          OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
      )
      AND (
          :unassigned IS NULL
          OR (:unassigned = true AND t.assignedAgent IS NULL)
          OR (:unassigned = false AND t.assignedAgent IS NOT NULL)
      )
      AND (
          :slaStatus IS NULL
          OR EXISTS (
              SELECT s
              FROM SlaInstance s
              WHERE s.ticket.id = t.id
                AND s.status = :slaStatus
                AND s.cycleNumber = (
                    SELECT MAX(s2.cycleNumber)
                    FROM SlaInstance s2
                    WHERE s2.ticket.id = t.id
                )
          )
      )
    """, countQuery = """
    SELECT COUNT(t)
    FROM Ticket t
    WHERE (:departmentId IS NULL OR t.department.id = :departmentId)
      AND (:status IS NULL OR t.status = :status)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND (
          :search IS NULL OR :search = ''
          OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
      )
      AND (
          :unassigned IS NULL
          OR (:unassigned = true AND t.assignedAgent IS NULL)
          OR (:unassigned = false AND t.assignedAgent IS NOT NULL)
      )
      AND (
          :slaStatus IS NULL
          OR EXISTS (
              SELECT s
              FROM SlaInstance s
              WHERE s.ticket.id = t.id
                AND s.status = :slaStatus
                AND s.cycleNumber = (
                    SELECT MAX(s2.cycleNumber)
                    FROM SlaInstance s2
                    WHERE s2.ticket.id = t.id
                )
          )
      )
    """)
    Page<Ticket> findDepartmentTicketsWithFilters(
            @Param("departmentId") Long departmentId,
            @Param("status") TicketStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("search") String search,
            @Param("unassigned") Boolean unassigned,
            @Param("slaStatus") SlaStatus slaStatus,
            Pageable pageable
    );

    @Query("""
    SELECT t.status, COUNT(t)
    FROM Ticket t
    WHERE t.department.id = :departmentId
    GROUP BY t.status
    """)
    List<Object[]> countTicketsByStatusForDepartment(@Param("departmentId") Long departmentId);

    @Query("""
    SELECT t.status, COUNT(t)
    FROM Ticket t
    GROUP BY t.status
    """)
    List<Object[]> countAllTicketsByStatus();

}