package xyz.mobi.employeehelpdesk.dto.ticket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdNameResponseDTO {

    private Long id;
    private String name;
}