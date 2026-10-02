package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hd_ticket_messages")
public class HDTicketMessageEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ticket_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_ticket")
    )
    private HDTicketEntity ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sender_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_sender")
    )
    private HDEmployeeEntity sender;

    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;
}