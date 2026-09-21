package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.holiday.HolidayRequest;
import xyz.mobi.employeehelpdesk.dto.holiday.HolidayResponse;
import xyz.mobi.employeehelpdesk.dto.holiday.HolidayUpdateRequest;
import xyz.mobi.employeehelpdesk.service.HolidayService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<HolidayResponse> createHoliday(
            @Valid @RequestBody HolidayRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(holidayService.createHoliday(request));
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<HolidayResponse>> getAllHolidays() {

        return ResponseEntity.ok(
                holidayService.getAllHolidays()
        );
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/{holidayDate}")
    public ResponseEntity<HolidayResponse> getHoliday(
            @PathVariable LocalDate holidayDate) {

        return ResponseEntity.ok(
                holidayService.getHoliday(holidayDate)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{holidayDate}")
    public ResponseEntity<HolidayResponse> updateHoliday(
            @PathVariable LocalDate holidayDate,
            @Valid @RequestBody HolidayUpdateRequest request) {

        return ResponseEntity.ok(
                holidayService.updateHoliday(
                        holidayDate,
                        request
                )
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{holidayDate}")
    public ResponseEntity<Void> deleteHoliday(
            @PathVariable LocalDate holidayDate) {

        holidayService.deleteHoliday(holidayDate);

        return ResponseEntity.noContent().build();
    }
}