package xyz.mobi.employeehelpdesk.dto.ticket;

import jakarta.validation.constraints.NotNull;
import xyz.mobi.employeehelpdesk.entity.enums.TicketPatchRequestType;

public record UpdateTicketRequest(

        @NotNull
        TicketPatchRequestType status,

        String reason,

        String resolution,

        Long agentId

) {
}