package xyz.mobi.employeehelpdesk.dto.holiday;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HolidayResponse(

        LocalDate holidayDate,

        String holidayName,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}
