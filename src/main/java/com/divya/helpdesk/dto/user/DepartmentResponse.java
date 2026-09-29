package com.divya.helpdesk.dto.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DepartmentResponse {

    private Long id;

    private String name;
}
