package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HDNotificationRepository extends JpaRepository<HDNotification, Long> {

    List<HDNotification> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId);
}
