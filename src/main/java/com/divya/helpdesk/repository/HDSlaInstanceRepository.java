package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDSlaInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDSlaInstanceRepository extends JpaRepository<HDSlaInstance, Long> {
    Optional<HDSlaInstance> findByTicketId(Long ticketId);
    List<HDSlaInstance> findAllByTicketId(Long ticketId);
}
