package xyz.mobi.employeehelpdesk.dto.ticket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignableAgentResponseDTO {

    private Long agentId;
    private Long employeeId;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private Long departmentId;
    private String departmentName;
    private int matchedSkillCount;
    private int requiredSkillCount;
    private double matchPercentage;
    private long activeTicketCount;
    private Instant lastAssignedAt;
}
