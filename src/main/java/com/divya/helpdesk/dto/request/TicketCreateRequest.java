package com.divya.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreateRequest {

    @NotNull(message = "Requester ID is required")
    private Long requesterId;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "SubCategory ID is required")
    private Long subCategoryId;

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;
}
