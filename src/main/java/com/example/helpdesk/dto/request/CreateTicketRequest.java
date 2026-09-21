package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketRequest {

    @NotNull
    private Long requesterId;

    @NotNull
    private Long departmentId;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long subCategoryId;

    @NotBlank
    private String subject;

    @NotBlank
    private String description;

    @NotBlank
    private String status;
}