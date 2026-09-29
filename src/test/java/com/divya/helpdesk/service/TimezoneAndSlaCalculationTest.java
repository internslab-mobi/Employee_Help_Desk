package com.divya.helpdesk.service;

import com.divya.helpdesk.entity.HDBusinessCalendar;
import com.divya.helpdesk.entity.HDBusinessHours;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.repository.HDBusinessCalendarRepository;
import com.divya.helpdesk.repository.HDBusinessHoursRepository;
import com.divya.helpdesk.repository.HDHolidayRepository;
import com.divya.helpdesk.service.impl.HDSlaCalculationServiceImpl;
import com.divya.helpdesk.util.TimezoneUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimezoneAndSlaCalculationTest {

    @Mock
    private HDBusinessCalendarRepository businessCalendarRepository;

    @Mock
    private HDBusinessHoursRepository businessHoursRepository;

    @Mock
    private HDHolidayRepository holidayRepository;

    @InjectMocks
    private HDSlaCalculationServiceImpl slaCalculationService;

    private TimeZone originalDefaultTimeZone;

    @BeforeEach
    void setUp() {
        originalDefaultTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    private HDBusinessHours createWorkingHours(HDBusinessCalendar calendar, DayOfWeek day, LocalTime start, LocalTime end) {
        HDBusinessHours hours = new HDBusinessHours();
        hours.setCalendar(calendar);
        hours.setDayOfWeek(day);
        hours.setWorkingDay(true);
        hours.setStartTime(start);
        hours.setEndTime(end);
        return hours;
    }

    private void mockStandardWorkWeek(Long calendarId, HDBusinessCalendar calendar, LocalTime start, LocalTime end) {
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                lenient().when(businessHoursRepository.findByCalendar_IdAndDayOfWeek(calendarId, day))
                        .thenReturn(Optional.empty());
            } else {
                HDBusinessHours hours = createWorkingHours(calendar, day, start, end);
                lenient().when(businessHoursRepository.findByCalendar_IdAndDayOfWeek(calendarId, day))
                        .thenReturn(Optional.of(hours));
            }
        }
    }

    @Test
    @DisplayName("Test 1: Employee & Calendar in Asia/Kolkata - SLA calculation behavior")
    void testSlaCalculation_KolkataCalendar() {
        Long calendarId = 1L;
        HDBusinessCalendar calendar = new HDBusinessCalendar();
        calendar.setId(calendarId);
        calendar.setName("India Support Calendar");
        calendar.setTimezone("Asia/Kolkata");

        lenient().when(businessCalendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));
        mockStandardWorkWeek(calendarId, calendar, LocalTime.of(8, 30), LocalTime.of(17, 30));
        lenient().when(holidayRepository.existsByCalendar_IdAndHolidayDate(eq(calendarId), any(LocalDate.class))).thenReturn(false);

        // Start: Wednesday 2026-09-23 16:00 Asia/Kolkata (10:30 UTC)
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 9, 23, 16, 0, 0, 0, ZoneId.of("Asia/Kolkata"));
        Instant startInstant = startZoned.toInstant();

        // 240 business minutes:
        // Day 1 (Wed): 16:00 to 17:30 = 90 mins. Remaining = 150 mins.
        // Day 2 (Thu): 08:30 + 150 mins = 11:00 AM Asia/Kolkata (05:30 UTC).
        Instant deadline = slaCalculationService.addWorkingMinutes(calendarId, startInstant, 240);

        ZonedDateTime deadlineKolkata = deadline.atZone(ZoneId.of("Asia/Kolkata"));
        assertEquals(LocalDate.of(2026, 9, 24), deadlineKolkata.toLocalDate());
        assertEquals(LocalTime.of(11, 0), deadlineKolkata.toLocalTime());
        assertEquals(Instant.parse("2026-09-24T05:30:00Z"), deadline);
    }

    @Test
    @DisplayName("Test 2: Employee in Asia/Kuala_Lumpur, Calendar in Asia/Kolkata - SLA uses Calendar zone, display converted to Employee zone")
    void testSlaCalculation_EmployeeKualaLumpur_CalendarKolkata() {
        Long calendarId = 1L;
        HDBusinessCalendar calendar = new HDBusinessCalendar();
        calendar.setId(calendarId);
        calendar.setTimezone("Asia/Kolkata");

        lenient().when(businessCalendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));
        mockStandardWorkWeek(calendarId, calendar, LocalTime.of(8, 30), LocalTime.of(17, 30));
        lenient().when(holidayRepository.existsByCalendar_IdAndHolidayDate(eq(calendarId), any(LocalDate.class))).thenReturn(false);

        // Created at 2026-09-24T04:30:00Z
        Instant ticketCreatedAt = Instant.parse("2026-09-24T04:30:00Z");

        // Employee A (Asia/Kolkata) view
        ZonedDateTime employeeATime = ticketCreatedAt.atZone(ZoneId.of("Asia/Kolkata"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a", Locale.ENGLISH);
        assertEquals("24 Sep 2026 10:00 AM", employeeATime.format(formatter));

        // Employee B (Asia/Kuala_Lumpur) view
        ZonedDateTime employeeBTime = ticketCreatedAt.atZone(ZoneId.of("Asia/Kuala_Lumpur"));
        assertEquals("24 Sep 2026 12:30 PM", employeeBTime.format(formatter));

        // Both view the EXACT same instant
        assertEquals(employeeATime.toInstant(), employeeBTime.toInstant());
        assertEquals(ticketCreatedAt, employeeATime.toInstant());
    }

    @Test
    @DisplayName("Test 3: Calendar in Asia/Kuala_Lumpur - SLA business hours calculated according to Malaysia time")
    void testSlaCalculation_KualaLumpurCalendar() {
        Long calendarId = 2L;
        HDBusinessCalendar calendar = new HDBusinessCalendar();
        calendar.setId(calendarId);
        calendar.setName("Malaysia Support Calendar");
        calendar.setTimezone("Asia/Kuala_Lumpur");

        lenient().when(businessCalendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));
        mockStandardWorkWeek(calendarId, calendar, LocalTime.of(8, 30), LocalTime.of(17, 30));
        lenient().when(holidayRepository.existsByCalendar_IdAndHolidayDate(eq(calendarId), any(LocalDate.class))).thenReturn(false);

        // Start: Wednesday 2026-09-23 16:00 Asia/Kuala_Lumpur (08:00 UTC)
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 9, 23, 16, 0, 0, 0, ZoneId.of("Asia/Kuala_Lumpur"));
        Instant startInstant = startZoned.toInstant();

        // 240 business minutes in Malaysia timezone:
        // Day 1: 16:00 to 17:30 = 90 mins. Remaining = 150 mins.
        // Day 2: 08:30 + 150 mins = 11:00 AM Asia/Kuala_Lumpur (03:00 UTC).
        Instant deadline = slaCalculationService.addWorkingMinutes(calendarId, startInstant, 240);

        ZonedDateTime deadlineKL = deadline.atZone(ZoneId.of("Asia/Kuala_Lumpur"));
        assertEquals(LocalDate.of(2026, 9, 24), deadlineKL.toLocalDate());
        assertEquals(LocalTime.of(11, 0), deadlineKL.toLocalTime());
        assertEquals(Instant.parse("2026-09-24T03:00:00Z"), deadline);
    }

    @Test
    @DisplayName("Test 4: Holiday on 2026-09-24 - SLA skips the holiday in calendar timezone")
    void testSlaCalculation_WithHoliday() {
        Long calendarId = 1L;
        HDBusinessCalendar calendar = new HDBusinessCalendar();
        calendar.setId(calendarId);
        calendar.setTimezone("Asia/Kolkata");

        lenient().when(businessCalendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));
        mockStandardWorkWeek(calendarId, calendar, LocalTime.of(8, 30), LocalTime.of(17, 30));

        // 2026-09-24 is a holiday in this calendar
        lenient().when(holidayRepository.existsByCalendar_IdAndHolidayDate(calendarId, LocalDate.of(2026, 9, 24)))
                .thenReturn(true);
        lenient().when(holidayRepository.existsByCalendar_IdAndHolidayDate(calendarId, LocalDate.of(2026, 9, 23)))
                .thenReturn(false);
        lenient().when(holidayRepository.existsByCalendar_IdAndHolidayDate(calendarId, LocalDate.of(2026, 9, 25)))
                .thenReturn(false);

        // Start: Wednesday 2026-09-23 16:00 Asia/Kolkata
        ZonedDateTime startZoned = ZonedDateTime.of(2026, 9, 23, 16, 0, 0, 0, ZoneId.of("Asia/Kolkata"));
        Instant startInstant = startZoned.toInstant();

        // 240 business minutes:
        // Day 1 (Wed 23 Sep): 16:00 to 17:30 = 90 mins. Remaining = 150 mins.
        // Day 2 (Thu 24 Sep): Holiday -> 0 mins counted.
        // Day 3 (Fri 25 Sep): 08:30 + 150 mins = 11:00 AM Asia/Kolkata (05:30 UTC).
        Instant deadline = slaCalculationService.addWorkingMinutes(calendarId, startInstant, 240);

        ZonedDateTime deadlineKolkata = deadline.atZone(ZoneId.of("Asia/Kolkata"));
        assertEquals(LocalDate.of(2026, 9, 25), deadlineKolkata.toLocalDate());
        assertEquals(LocalTime.of(11, 0), deadlineKolkata.toLocalTime());
        assertEquals(Instant.parse("2026-09-25T05:30:00Z"), deadline);
    }

    @Test
    @DisplayName("Test 5: Server/JVM timezone independence - Changing JVM timezone does not alter absolute deadline Instant")
    void testSlaCalculation_JvmTimezoneIndependence() {
        Long calendarId = 1L;
        HDBusinessCalendar calendar = new HDBusinessCalendar();
        calendar.setId(calendarId);
        calendar.setTimezone("Asia/Kolkata");

        lenient().when(businessCalendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));
        mockStandardWorkWeek(calendarId, calendar, LocalTime.of(8, 30), LocalTime.of(17, 30));
        lenient().when(holidayRepository.existsByCalendar_IdAndHolidayDate(eq(calendarId), any(LocalDate.class))).thenReturn(false);

        Instant startInstant = ZonedDateTime.of(2026, 9, 23, 16, 0, 0, 0, ZoneId.of("Asia/Kolkata")).toInstant();

        // Run calculation under America/New_York JVM timezone
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        Instant deadlineUnderNy = slaCalculationService.addWorkingMinutes(calendarId, startInstant, 240);

        // Run calculation under UTC JVM timezone
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Instant deadlineUnderUtc = slaCalculationService.addWorkingMinutes(calendarId, startInstant, 240);

        // Run calculation under Pacific/Auckland JVM timezone
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"));
        Instant deadlineUnderAuckland = slaCalculationService.addWorkingMinutes(calendarId, startInstant, 240);

        // All must produce identical absolute Instant (2026-09-24T05:30:00Z)
        assertEquals(Instant.parse("2026-09-24T05:30:00Z"), deadlineUnderNy);
        assertEquals(Instant.parse("2026-09-24T05:30:00Z"), deadlineUnderUtc);
        assertEquals(Instant.parse("2026-09-24T05:30:00Z"), deadlineUnderAuckland);
    }

    @Test
    @DisplayName("Test 6: Timezone Validation - Valid IANA vs Invalid aliases/formats")
    void testTimezoneValidation() {
        // Valid IANA IDs
        assertTrue(TimezoneUtil.isValidTimezone("Asia/Kolkata"));
        assertTrue(TimezoneUtil.isValidTimezone("Asia/Kuala_Lumpur"));
        assertTrue(TimezoneUtil.isValidTimezone("Asia/Singapore"));
        assertTrue(TimezoneUtil.isValidTimezone("Europe/London"));
        assertTrue(TimezoneUtil.isValidTimezone("America/New_York"));
        assertTrue(TimezoneUtil.isValidTimezone("UTC"));

        assertDoesNotThrow(() -> TimezoneUtil.validateAndGetZoneId("Asia/Kolkata"));
        assertDoesNotThrow(() -> TimezoneUtil.validateAndGetZoneId("Asia/Kuala_Lumpur"));

        // Invalid non-IANA formats and abbreviations
        assertFalse(TimezoneUtil.isValidTimezone("IST"));
        assertFalse(TimezoneUtil.isValidTimezone("GMT+5:30"));
        assertFalse(TimezoneUtil.isValidTimezone("UTC+05:30"));
        assertFalse(TimezoneUtil.isValidTimezone("India"));
        assertFalse(TimezoneUtil.isValidTimezone("Malaysia Time"));
        assertFalse(TimezoneUtil.isValidTimezone("Invalid/Timezone"));
        assertFalse(TimezoneUtil.isValidTimezone(""));
        assertFalse(TimezoneUtil.isValidTimezone(null));

        assertThrows(BadRequestException.class, () -> TimezoneUtil.validateAndGetZoneId("IST"));
        assertThrows(BadRequestException.class, () -> TimezoneUtil.validateAndGetZoneId("GMT+5:30"));
        assertThrows(BadRequestException.class, () -> TimezoneUtil.validateAndGetZoneId("UTC+05:30"));
        assertThrows(BadRequestException.class, () -> TimezoneUtil.validateAndGetZoneId("India"));
        assertThrows(BadRequestException.class, () -> TimezoneUtil.validateAndGetZoneId("Malaysia Time"));
        assertThrows(BadRequestException.class, () -> TimezoneUtil.validateAndGetZoneId("Invalid/Timezone"));
        assertThrows(BadRequestException.class, () -> TimezoneUtil.validateAndGetZoneId(""));
        assertThrows(BadRequestException.class, () -> TimezoneUtil.validateAndGetZoneId(null));
    }
}
