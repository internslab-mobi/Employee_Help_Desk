package xyz.mobi.employeehelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.holiday.CreateHolidayRequestDTO;
import xyz.mobi.employeehelpdesk.dto.holiday.HolidayResponseDTO;
import xyz.mobi.employeehelpdesk.service.HolidayService;

import java.util.List;
import java.util.Map;

@Tag(name = "Holidays", description = "Holiday Calendar configuration and retrieval endpoints")
@RestController
@RequestMapping("/helpdesk/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @Operation(summary = "Get holidays, optionally filtered by department ID and year")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<HolidayResponseDTO>> getHolidays(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year
    ) {
        List<HolidayResponseDTO> holidays = holidayService.getHolidays(departmentId, year);
        return ResponseEntity.ok(holidays);
    }

    @Operation(summary = "Create a new holiday (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<HolidayResponseDTO> createHoliday(
            @Valid @RequestBody CreateHolidayRequestDTO request
    ) {
        HolidayResponseDTO holiday = holidayService.createHoliday(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(holiday);
    }

    @Operation(summary = "Delete holiday by ID (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.ok(Map.of("message", "Holiday deleted successfully"));
    }
}
