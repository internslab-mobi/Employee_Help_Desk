package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.Notification;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(
            Long recipientId
    );

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(
            Long recipientId,
            Pageable pageable
    );

    Page<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(
            Long recipientId,
            Pageable pageable
    );

    long countByRecipientIdAndReadFalse(Long recipientId);
}