package xyz.mobi.employeehelpdesk.dto.holiday;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record HolidayResponseDTO(
        Long id,
        Long departmentId,
        String departmentName,
        LocalDate holidayDate,
        String holidayName
) {}
