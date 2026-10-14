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
            "assignedAgent", "assignedAgent.employee", "assignedManager"})
    Optional<Ticket> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager"})
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
            "assignedAgent", "assignedAgent.employee", "assignedManager"})
    Page<Ticket> findByRequesterId(
            Long requesterId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager"})
    Page<Ticket> findByAssignedAgentId(
            Long agentId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager"})
    Page<Ticket> findByDepartmentId(
            Long departmentId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager"})
    @Query("SELECT t FROM Ticket t")
    Page<Ticket> findAllTickets(
            Pageable pageable
    );

    // --- SEARCH Queries with combined filters ---

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager"})
    @Query(value = """
    SELECT t
    FROM Ticket t
    WHERE t.requester.id = :requesterId
      AND (:status IS NULL OR t.status = :status)
      AND (:priority IS NULL OR t.priority = :priority)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND (
          :search IS NULL OR :search = ''
          OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))
          OR (t.category IS NOT NULL AND LOWER(t.category.name) LIKE LOWER(CONCAT('%', :search, '%')))
          OR (t.requester IS NOT NULL AND (
              LOWER(t.requester.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(t.requester.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
          ))
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
      AND (:priority IS NULL OR t.priority = :priority)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND (
          :search IS NULL OR :search = ''
          OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))
          OR (t.category IS NOT NULL AND LOWER(t.category.name) LIKE LOWER(CONCAT('%', :search, '%')))
          OR (t.requester IS NOT NULL AND (
              LOWER(t.requester.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(t.requester.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
          ))
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
            @Param("priority") Priority priority,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("search") String search,
            @Param("unassigned") Boolean unassigned,
            @Param("slaStatus") SlaStatus slaStatus,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager"})
    @Query(value = """
    SELECT t
    FROM Ticket t
    WHERE t.assignedAgent.id = :agentId
      AND (:status IS NULL OR t.status = :status)
      AND (:priority IS NULL OR t.priority = :priority)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND (
          :search IS NULL OR :search = ''
          OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))
          OR (t.category IS NOT NULL AND LOWER(t.category.name) LIKE LOWER(CONCAT('%', :search, '%')))
          OR (t.requester IS NOT NULL AND (
              LOWER(t.requester.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(t.requester.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
          ))
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
      AND (:priority IS NULL OR t.priority = :priority)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND (
          :search IS NULL OR :search = ''
          OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))
          OR (t.category IS NOT NULL AND LOWER(t.category.name) LIKE LOWER(CONCAT('%', :search, '%')))
          OR (t.requester IS NOT NULL AND (
              LOWER(t.requester.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(t.requester.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
          ))
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
            @Param("priority") Priority priority,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("search") String search,
            @Param("unassigned") Boolean unassigned,
            @Param("slaStatus") SlaStatus slaStatus,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee", "assignedManager"})
    @Query(value = """
    SELECT t
    FROM Ticket t
    WHERE (:departmentId IS NULL OR t.department.id = :departmentId)
      AND (:agentId IS NULL OR (t.assignedAgent IS NOT NULL AND t.assignedAgent.id = :agentId))
      AND (:status IS NULL OR t.status = :status)
      AND (:priority IS NULL OR t.priority = :priority)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND (
          :search IS NULL OR :search = ''
          OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))
          OR (t.category IS NOT NULL AND LOWER(t.category.name) LIKE LOWER(CONCAT('%', :search, '%')))
          OR (t.requester IS NOT NULL AND (
              LOWER(t.requester.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(t.requester.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
          ))
          OR (t.assignedAgent IS NOT NULL AND t.assignedAgent.employee IS NOT NULL AND (
              LOWER(t.assignedAgent.employee.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(t.assignedAgent.employee.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
          ))
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
      AND (:agentId IS NULL OR (t.assignedAgent IS NOT NULL AND t.assignedAgent.id = :agentId))
      AND (:status IS NULL OR t.status = :status)
      AND (:priority IS NULL OR t.priority = :priority)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND (
          :search IS NULL OR :search = ''
          OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))
          OR (t.category IS NOT NULL AND LOWER(t.category.name) LIKE LOWER(CONCAT('%', :search, '%')))
          OR (t.requester IS NOT NULL AND (
              LOWER(t.requester.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(t.requester.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
          ))
          OR (t.assignedAgent IS NOT NULL AND t.assignedAgent.employee IS NOT NULL AND (
              LOWER(t.assignedAgent.employee.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(t.assignedAgent.employee.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
          ))
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
            @Param("agentId") Long agentId,
            @Param("status") TicketStatus status,
            @Param("priority") Priority priority,
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

    @Query("""
    SELECT t.status, COUNT(t)
    FROM Ticket t
    WHERE (:requesterId IS NULL OR t.requester.id = :requesterId)
      AND (:agentId IS NULL OR t.assignedAgent.id = :agentId)
      AND (:departmentId IS NULL OR t.department.id = :departmentId)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
    GROUP BY t.status
    """)
    List<Object[]> countScopedTicketsByStatus(
            @Param("requesterId") Long requesterId,
            @Param("agentId") Long agentId,
            @Param("departmentId") Long departmentId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
    SELECT t.priority, COUNT(t)
    FROM Ticket t
    WHERE (:requesterId IS NULL OR t.requester.id = :requesterId)
      AND (:agentId IS NULL OR t.assignedAgent.id = :agentId)
      AND (:departmentId IS NULL OR t.department.id = :departmentId)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
    GROUP BY t.priority
    """)
    List<Object[]> countScopedTicketsByPriority(
            @Param("requesterId") Long requesterId,
            @Param("agentId") Long agentId,
            @Param("departmentId") Long departmentId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
    SELECT t.department.id, t.department.name, COUNT(t),
           SUM(CASE WHEN t.status IN ('OPEN', 'IN_PROGRESS', 'ON_HOLD', 'REOPENED') THEN 1 ELSE 0 END),
           SUM(CASE WHEN t.status = 'RESOLVED' THEN 1 ELSE 0 END)
    FROM Ticket t
    WHERE (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
    GROUP BY t.department.id, t.department.name
    ORDER BY COUNT(t) DESC
    """)
    List<Object[]> countTicketsByDepartment(
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
    SELECT COALESCE(t.category.name, 'Uncategorized'), COUNT(t)
    FROM Ticket t
    WHERE (:departmentId IS NULL OR t.department.id = :departmentId)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
    GROUP BY t.category.name
    ORDER BY COUNT(t) DESC
    """)
    List<Object[]> countTicketsByCategory(
            @Param("departmentId") Long departmentId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
    SELECT COUNT(DISTINCT t)
    FROM Ticket t
    JOIN SlaInstance s ON s.ticket = t
    WHERE (:requesterId IS NULL OR t.requester.id = :requesterId)
      AND (:agentId IS NULL OR t.assignedAgent.id = :agentId)
      AND (:departmentId IS NULL OR t.department.id = :departmentId)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
      AND s.status = :slaStatus
      AND s.cycleNumber = (
          SELECT MAX(s2.cycleNumber)
          FROM SlaInstance s2
          WHERE s2.ticket.id = t.id
      )
    """)
    long countTicketsByLatestSlaStatus(
            @Param("requesterId") Long requesterId,
            @Param("agentId") Long agentId,
            @Param("departmentId") Long departmentId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("slaStatus") SlaStatus slaStatus
    );

    @Query("""
    SELECT
        SUM(CASE WHEN t.assignedAgent IS NOT NULL THEN 1 ELSE 0 END),
        SUM(CASE WHEN t.assignedAgent IS NULL THEN 1 ELSE 0 END)
    FROM Ticket t
    WHERE (:departmentId IS NULL OR t.department.id = :departmentId)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
    """)
    List<Object[]> countAssignedAndUnassignedTickets(
            @Param("departmentId") Long departmentId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
    SELECT COUNT(t)
    FROM Ticket t
    WHERE t.requester.id = :requesterId
      AND t.assignedAgent IS NOT NULL
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
    """)
    long countAssignedTicketsForRequester(
            @Param("requesterId") Long requesterId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
    SELECT t.createdAt, t.resolvedAt
    FROM Ticket t
    WHERE t.resolvedAt IS NOT NULL
      AND (:requesterId IS NULL OR t.requester.id = :requesterId)
      AND (:agentId IS NULL OR t.assignedAgent.id = :agentId)
      AND (:departmentId IS NULL OR t.department.id = :departmentId)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
    """)
    List<Object[]> findResolutionIntervals(
            @Param("requesterId") Long requesterId,
            @Param("agentId") Long agentId,
            @Param("departmentId") Long departmentId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
    SELECT t.createdAt, t.resolvedAt
    FROM Ticket t
    WHERE (:requesterId IS NULL OR t.requester.id = :requesterId)
      AND (:agentId IS NULL OR t.assignedAgent.id = :agentId)
      AND (:departmentId IS NULL OR t.department.id = :departmentId)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
    ORDER BY t.createdAt ASC
    """)
    List<Object[]> findTimestampsForTrend(
            @Param("requesterId") Long requesterId,
            @Param("agentId") Long agentId,
            @Param("departmentId") Long departmentId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @EntityGraph(attributePaths = {"requester", "department", "category", "subCategory",
            "assignedAgent", "assignedAgent.employee"})
    @Query("""
    SELECT t
    FROM Ticket t
    WHERE (:requesterId IS NULL OR t.requester.id = :requesterId)
      AND (:agentId IS NULL OR t.assignedAgent.id = :agentId)
      AND (:departmentId IS NULL OR t.department.id = :departmentId)
      AND (CAST(:from AS java.time.Instant) IS NULL OR t.createdAt >= :from)
      AND (CAST(:to AS java.time.Instant) IS NULL OR t.createdAt < :to)
    ORDER BY t.createdAt DESC
    """)
    List<Ticket> findScopedRecentTickets(
            @Param("requesterId") Long requesterId,
            @Param("agentId") Long agentId,
            @Param("departmentId") Long departmentId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}