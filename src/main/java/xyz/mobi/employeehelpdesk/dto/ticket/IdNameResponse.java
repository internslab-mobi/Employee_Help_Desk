package xyz.mobi.employeehelpdesk.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class IdNameResponse {

    private Long id;
    private String name;
}