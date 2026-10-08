package xyz.mobi.employeehelpdesk.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class IdNameResponseDTO {

    private Long id;
    private String name;
}