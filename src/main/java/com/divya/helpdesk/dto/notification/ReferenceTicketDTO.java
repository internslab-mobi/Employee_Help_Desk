package com.divya.helpdesk.dto.notification;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ReferenceTicketDTO {

    private Long id;
    private String ticketNumber;
}
