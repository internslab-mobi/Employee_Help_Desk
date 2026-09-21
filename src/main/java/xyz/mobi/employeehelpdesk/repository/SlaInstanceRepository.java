package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;
import xyz.mobi.employeehelpdesk.entity.SlaInstance;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SlaInstanceRepository
        extends JpaRepository<SlaInstance, Long> {

    List<SlaInstance> findByTicketIdOrderByCycleNumberAsc(
            Long ticketId
    );

    Optional<SlaInstance> findByTicketIdAndCycleNumber(
            Long ticketId,
            Integer cycleNumber
    );

    List<SlaInstance> findByStatus(SlaStatus status);

    Optional<SlaInstance> findTopByTicketIdOrderByCycleNumberDesc(
            Long ticketId
    );

    List<SlaInstance> findByTicketIdIn(List<Long> ticketIds);

    @Query("""
    SELECT s
    FROM SlaInstance s
    WHERE s.ticket.id IN :ticketIds
      AND s.cycleNumber = (
          SELECT MAX(s2.cycleNumber)
          FROM SlaInstance s2
          WHERE s2.ticket.id = s.ticket.id
      )
""")
    List<SlaInstance> findLatestByTicketIds(
            @Param("ticketIds") List<Long> ticketIds
    );

    @Query("""
    SELECT s
    FROM SlaInstance s
    WHERE s.ticket.id = :ticketId
      AND s.cycleNumber = (
          SELECT MAX(s2.cycleNumber)
          FROM SlaInstance s2
          WHERE s2.ticket.id = :ticketId
      )
""")
    SlaInstance findLatestByTicketId(
            @Param("ticketId") Long ticketId
    );

    /**
     * Keyset-based query for breach candidates.
     * Fetches SLA instances that are past their deadline, ordered by ID for stable cursor pagination.
     */
    @Query("""
        SELECT s
        FROM SlaInstance s
        JOIN FETCH s.ticket t
        WHERE s.status IN :statuses
          AND s.currentDeadlineAt <= :now
          AND s.id > :lastProcessedId
        ORDER BY s.id ASC
    """)
    List<SlaInstance> findSlasDueForBreach(
            @Param("statuses") Collection<SlaStatus> statuses,
            @Param("now") LocalDateTime now,
            @Param("lastProcessedId") Long lastProcessedId,
            @Param("batchSize") int batchSize
    );

    /**
     * Spring Data compatible version using Pageable for breach candidates with keyset.
     */
    @Query("""
        SELECT s
        FROM SlaInstance s
        JOIN FETCH s.ticket t
        WHERE s.status IN :statuses
          AND s.currentDeadlineAt <= :now
          AND s.id > :lastProcessedId
        ORDER BY s.id ASC
    """)
    List<SlaInstance> findBreachCandidatesAfter(
            @Param("statuses") Collection<SlaStatus> statuses,
            @Param("now") LocalDateTime now,
            @Param("lastProcessedId") Long lastProcessedId,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * Keyset-based query for warning candidates.
     */
    @Query("""
        SELECT s
        FROM SlaInstance s
        JOIN FETCH s.ticket t
        WHERE s.status = :status
          AND s.warningAt IS NOT NULL
          AND s.warningAt <= :now
          AND s.currentDeadlineAt > :now
          AND s.id > :lastProcessedId
        ORDER BY s.id ASC
    """)
    List<SlaInstance> findWarningCandidatesAfter(
            @Param("status") SlaStatus status,
            @Param("now") LocalDateTime now,
            @Param("lastProcessedId") Long lastProcessedId,
            org.springframework.data.domain.Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE SlaInstance s
        SET s.status = :newStatus
        WHERE s.id = :id
          AND s.status = :expectedStatus
          AND s.warningAt IS NOT NULL
          AND s.warningAt <= :now
          AND s.currentDeadlineAt > :now
    """)
    int updateStatusToWarningIfEligible(
            @Param("id") Long id,
            @Param("newStatus") SlaStatus newStatus,
            @Param("expectedStatus") SlaStatus expectedStatus,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE SlaInstance s
        SET s.status = :newStatus,
            s.breachedAt = :now
        WHERE s.id = :id
          AND s.status IN :expectedStatuses
          AND s.currentDeadlineAt <= :now
    """)
    int updateStatusToBreachedIfEligible(
            @Param("id") Long id,
            @Param("newStatus") SlaStatus newStatus,
            @Param("expectedStatuses") Collection<SlaStatus> expectedStatuses,
            @Param("now") LocalDateTime now
    );

    boolean existsBySlaPolicyId(Long slaPolicyId);
}