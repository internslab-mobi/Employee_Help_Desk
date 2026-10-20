package xyz.mobi.employeehelpdesk.dto.holiday;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CreateHolidayRequestDTO(
        @NotNull(message = "Department ID is required")
        Long departmentId,

        @NotNull(message = "Holiday date is required")
        LocalDate holidayDate,

        @NotBlank(message = "Holiday name is required")
        @Size(max = 100, message = "Holiday name cannot exceed 100 characters")
        String holidayName
) {}
