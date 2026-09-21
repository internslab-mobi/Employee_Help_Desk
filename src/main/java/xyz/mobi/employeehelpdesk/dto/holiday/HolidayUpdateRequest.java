package xyz.mobi.employeehelpdesk.dto.holiday;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HolidayUpdateRequest(

        @NotBlank(message = "Holiday name is required")
        @Size(max = 100, message = "Holiday name must not exceed 100 characters")
        String holidayName
) {
}