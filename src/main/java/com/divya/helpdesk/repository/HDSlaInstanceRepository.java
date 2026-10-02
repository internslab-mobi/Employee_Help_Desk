package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDSlaInstanceEntity;
import com.divya.helpdesk.enums.SlaInstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDSlaInstanceRepository extends JpaRepository<HDSlaInstanceEntity, Long> {

    Optional<HDSlaInstanceEntity> findByTicketId(Long ticketId);

    List<HDSlaInstanceEntity> findByStatus(SlaInstanceStatus status);
}