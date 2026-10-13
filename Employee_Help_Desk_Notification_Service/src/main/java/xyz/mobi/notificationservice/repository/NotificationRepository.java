package xyz.mobi.notificationservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.mobi.notificationservice.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(
            Long recipientId,
            Pageable pageable
    );

    Page<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(
            Long recipientId,
            Pageable pageable
    );

    Long countByRecipientIdAndReadFalse(Long recipientId);

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Notification> findByReadFalseOrderByCreatedAtDesc(Pageable pageable);

    Long countByReadFalse();
}
