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
    @JsonSubTypes.Type(value = StatusUpdateData.class, name = "STATUS"),
    @JsonSubTypes.Type(value = PriorityUpdateData.class, name = "PRIORITY"),
    @JsonSubTypes.Type(value = CategoryUpdateData.class, name = "CATEGORY"),
    @JsonSubTypes.Type(value = AssignAgentData.class, name = "ASSIGN_AGENT"),
    @JsonSubTypes.Type(value = AssignManagerData.class, name = "ASSIGN_MANAGER"),
    @JsonSubTypes.Type(value = HoldData.class, name = "HOLD"),
    @JsonSubTypes.Type(value = ResumeData.class, name = "RESUME"),
    @JsonSubTypes.Type(value = ResolveData.class, name = "RESOLVE"),
    @JsonSubTypes.Type(value = ReopenData.class, name = "REOPEN"),
    @JsonSubTypes.Type(value = WithdrawData.class, name = "WITHDRAW")
})
@Schema(description = "Base interface for operation-specific data", oneOf = {
    StatusUpdateData.class, PriorityUpdateData.class, CategoryUpdateData.class,
    AssignAgentData.class, AssignManagerData.class, HoldData.class, ResumeData.class,
    ResolveData.class, ReopenData.class, WithdrawData.class
})
public interface TicketOperationData {
}
