package xyz.mobi.employeehelpdesk.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.SlaInstance;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SlaInstanceRepository
        extends JpaRepository<SlaInstance, Long> {

    Optional<SlaInstance> findTopByTicketIdOrderByCycleNumberDesc(
            Long ticketId
    );

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SlaInstance s WHERE s.id = :id")
    Optional<SlaInstance> findByIdWithLock(@Param("id") Long id);

    @Query("""
        SELECT s
        FROM SlaInstance s
        WHERE s.status IN :statuses
          AND s.nextEventType IS NOT NULL
          AND s.nextEventAt IS NOT NULL
        ORDER BY s.nextEventAt ASC
    """)
    List<SlaInstance> findPendingSlaEvents(
            @Param("statuses") Collection<SlaStatus> statuses
    );

}