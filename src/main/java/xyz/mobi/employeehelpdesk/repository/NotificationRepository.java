package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.Notification;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"ticket", "recipient"})
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(
            Long recipientId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"ticket", "recipient"})
    Page<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(
            Long recipientId,
            Pageable pageable
    );

    long countByRecipientIdAndReadFalse(Long recipientId);
}