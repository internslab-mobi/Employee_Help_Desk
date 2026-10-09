package com.example.helpdesk.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "operationType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = StatusUpdateDataRequestDTO.class, name = "STATUS"),
    @JsonSubTypes.Type(value = PriorityUpdateDataRequestDTO.class, name = "PRIORITY"),
    @JsonSubTypes.Type(value = CategoryUpdateDataRequestDTO.class, name = "CATEGORY"),
    @JsonSubTypes.Type(value = AssignAgentDataRequestDTO.class, name = "ASSIGN_AGENT"),
    @JsonSubTypes.Type(value = AssignManagerDataRequestDTO.class, name = "ASSIGN_MANAGER"),
    @JsonSubTypes.Type(value = HoldDataRequestDTO.class, name = "HOLD"),
    @JsonSubTypes.Type(value = ResumeDataRequestDTO.class, name = "RESUME"),
    @JsonSubTypes.Type(value = ResolveDataRequestDTO.class, name = "RESOLVE"),
    @JsonSubTypes.Type(value = ReopenDataRequestDTO.class, name = "REOPEN"),
    @JsonSubTypes.Type(value = WithdrawDataRequestDTO.class, name = "WITHDRAW")
})
@Schema(description = "Base interface for operation-specific data", oneOf = {
    StatusUpdateDataRequestDTO.class, PriorityUpdateDataRequestDTO.class, CategoryUpdateDataRequestDTO.class,
    AssignAgentDataRequestDTO.class, AssignManagerDataRequestDTO.class, HoldDataRequestDTO.class, ResumeDataRequestDTO.class,
    ResolveDataRequestDTO.class, ReopenDataRequestDTO.class, WithdrawDataRequestDTO.class
})
public interface TicketOperationDataRequestDTO {
}
