package com.divya.helpdesk.dto.ticket;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageSenderDTO {

    private Long id;
    private String name;
    private String role;
}
