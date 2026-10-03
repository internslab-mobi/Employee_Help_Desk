package com.example.helpdesk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "hd_ticket_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Employee actor;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(columnDefinition = "text")
    private String oldValue;

    @Column(columnDefinition = "text")
    private String newValue;

    @Column(columnDefinition = "json")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
