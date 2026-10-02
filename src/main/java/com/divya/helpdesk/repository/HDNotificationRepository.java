package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HDNotificationRepository extends JpaRepository<HDNotificationEntity, Long> {

    List<HDNotificationEntity> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
}
