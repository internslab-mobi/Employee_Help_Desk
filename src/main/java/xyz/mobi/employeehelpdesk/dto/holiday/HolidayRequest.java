package xyz.mobi.employeehelpdesk.dto.holiday;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HolidayRequest(

        @NotNull(message = "Holiday date is required")
        LocalDate holidayDate,

        @NotBlank(message = "Holiday name is required")
        @Size(max = 100, message = "Holiday name must not exceed 100 characters")
        String holidayName
) {
}