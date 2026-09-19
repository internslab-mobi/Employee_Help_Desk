package com.divya.helpdesk.controller;

import com.divya.helpdesk.dto.request.BusinessCalendarRequest;
import com.divya.helpdesk.dto.request.BusinessHoursRequest;
import com.divya.helpdesk.dto.request.HolidayRequest;
import com.divya.helpdesk.dto.response.BusinessCalendarResponse;
import com.divya.helpdesk.dto.response.BusinessHoursResponse;
import com.divya.helpdesk.dto.response.HolidayResponse;
import com.divya.helpdesk.service.BusinessCalendarService;
import com.divya.helpdesk.service.BusinessHoursService;
import com.divya.helpdesk.service.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class BusinessCalendarController {

    private final BusinessCalendarService calendarService;
    private final BusinessHoursService businessHoursService;
    private final HolidayService holidayService;

    // --- Business Calendar Endpoints ---

    @PostMapping
    public ResponseEntity<BusinessCalendarResponse> createCalendar(@Valid @RequestBody BusinessCalendarRequest request) {
        BusinessCalendarResponse response = calendarService.createCalendar(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BusinessCalendarResponse>> getAllCalendars() {
        return ResponseEntity.ok(calendarService.getAllCalendars());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessCalendarResponse> getCalendarById(@PathVariable Long id) {
        return ResponseEntity.ok(calendarService.getCalendarById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessCalendarResponse> updateCalendar(@PathVariable Long id, @Valid @RequestBody BusinessCalendarRequest request) {
        return ResponseEntity.ok(calendarService.updateCalendar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCalendar(@PathVariable Long id) {
        calendarService.deleteCalendar(id);
        return ResponseEntity.noContent().build();
    }

    // --- Business Hours Endpoints ---

    @PostMapping("/{calendarId}/business-hours")
    public ResponseEntity<BusinessHoursResponse> createBusinessHours(
            @PathVariable Long calendarId,
            @Valid @RequestBody BusinessHoursRequest request) {
        BusinessHoursResponse response = businessHoursService.createBusinessHours(calendarId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{calendarId}/business-hours")
    public ResponseEntity<List<BusinessHoursResponse>> getBusinessHoursByCalendar(@PathVariable Long calendarId) {
        return ResponseEntity.ok(businessHoursService.getBusinessHoursByCalendarId(calendarId));
    }

    @GetMapping("/business-hours/{id}")
    public ResponseEntity<BusinessHoursResponse> getBusinessHoursById(@PathVariable Long id) {
        return ResponseEntity.ok(businessHoursService.getBusinessHoursById(id));
    }

    @PutMapping("/business-hours/{id}")
    public ResponseEntity<BusinessHoursResponse> updateBusinessHours(
            @PathVariable Long id,
            @Valid @RequestBody BusinessHoursRequest request) {
        return ResponseEntity.ok(businessHoursService.updateBusinessHours(id, request));
    }

    @DeleteMapping("/business-hours/{id}")
    public ResponseEntity<Void> deleteBusinessHours(@PathVariable Long id) {
        businessHoursService.deleteBusinessHours(id);
        return ResponseEntity.noContent().build();
    }

    // --- Holiday Endpoints ---

    @PostMapping("/{calendarId}/holidays")
    public ResponseEntity<HolidayResponse> createHoliday(
            @PathVariable Long calendarId,
            @Valid @RequestBody HolidayRequest request) {
        HolidayResponse response = holidayService.createHoliday(calendarId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{calendarId}/holidays")
    public ResponseEntity<List<HolidayResponse>> getHolidaysByCalendar(@PathVariable Long calendarId) {
        return ResponseEntity.ok(holidayService.getHolidaysByCalendarId(calendarId));
    }

    @GetMapping("/holidays/{id}")
    public ResponseEntity<HolidayResponse> getHolidayById(@PathVariable Long id) {
        return ResponseEntity.ok(holidayService.getHolidayById(id));
    }

    @PutMapping("/holidays/{id}")
    public ResponseEntity<HolidayResponse> updateHoliday(
            @PathVariable Long id,
            @Valid @RequestBody HolidayRequest request) {
        return ResponseEntity.ok(holidayService.updateHoliday(id, request));
    }

    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }
}
