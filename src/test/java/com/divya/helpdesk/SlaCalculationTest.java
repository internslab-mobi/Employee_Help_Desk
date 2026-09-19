package com.divya.helpdesk;

import com.divya.helpdesk.dto.request.*;
import com.divya.helpdesk.dto.response.BusinessCalendarResponse;
import com.divya.helpdesk.dto.response.DepartmentResponse;
import com.divya.helpdesk.dto.response.SlaPolicyResponse;
import com.divya.helpdesk.entity.HDBusinessCalendar;
import com.divya.helpdesk.entity.HDSlaInstance;
import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.repository.HDBusinessCalendarRepository;
import com.divya.helpdesk.service.BusinessCalendarService;
import com.divya.helpdesk.service.BusinessHoursService;
import com.divya.helpdesk.service.HolidayService;
import com.divya.helpdesk.service.SlaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class SlaCalculationTest {

    @Autowired
    private BusinessCalendarService calendarService;

    @Autowired
    private BusinessHoursService businessHoursService;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private HDBusinessCalendarRepository calendarRepository;

    @Autowired
    private SlaService slaService;

    @Autowired
    private HDSlaCalculationService slaCalculationService;

    private HDBusinessCalendar calendar;

    @BeforeEach
    void setUp() {
        // 1. Create Business Calendar
        BusinessCalendarResponse calResp = calendarService.createCalendar(new BusinessCalendarRequest(
                "STD_CAL", "Standard 9-5 Calendar", "Mon-Fri 9am to 5pm", "UTC", true, true
        ));

        // 2. Add Business Hours: Monday to Friday 09:00 to 17:00
        for (DayOfWeek dow : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            businessHoursService.createBusinessHours(calResp.getId(), new BusinessHoursRequest(
                    dow, LocalTime.of(9, 0), LocalTime.of(17, 0), true
            ));
        }
        // Saturday & Sunday: Non-working
        businessHoursService.createBusinessHours(calResp.getId(), new BusinessHoursRequest(
                DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), false
        ));
        businessHoursService.createBusinessHours(calResp.getId(), new BusinessHoursRequest(
                DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), false
        ));

        // 3. Add a Holiday on Monday 2026-09-21
        holidayService.createHoliday(calResp.getId(), new HolidayRequest(
                LocalDate.of(2026, 9, 21), "Company Holiday", "Office closed"
        ));

        calendar = calendarRepository.findById(calResp.getId()).orElseThrow();
    }

    @Test
    void testSameDayBusinessHoursCalculation() {
        // Wednesday 2026-09-16 at 10:00 AM + 120 minutes (2 hours)
        LocalDateTime start = LocalDateTime.of(2026, 9, 16, 10, 0);
        LocalDateTime deadline = slaService.calculateDueAt(calendar, start, 120);

        // Should be Wednesday 2026-09-16 at 12:00 PM
        assertEquals(LocalDateTime.of(2026, 9, 16, 12, 0), deadline);
        assertEquals(deadline, slaCalculationService.calculateDueAt(calendar, start, 120));
    }

    @Test
    void testWeekendAndHolidaySkippingWith75PercentWarningTime() {
        // Friday 2026-09-18 at 16:00 (4:00 PM) -> 1 hour left before close at 17:00
        // Duration = 120 minutes (2 hours)
        // Saturday 2026-09-19 -> Weekend (skipped)
        // Sunday 2026-09-20 -> Weekend (skipped)
        // Monday 2026-09-21 -> Holiday (skipped)
        // Tuesday 2026-09-22 -> Work starts at 09:00, remaining 60 minutes -> 10:00 AM!
        LocalDateTime start = LocalDateTime.of(2026, 9, 18, 16, 0);

        LocalDateTime deadline = slaService.calculateDueAt(calendar, start, 120);
        assertEquals(LocalDateTime.of(2026, 9, 22, 10, 0), deadline);
        assertEquals(deadline, slaCalculationService.calculateDueAt(calendar, start, 120));

        // Warning time (75% consumed of 120 minutes = 90 minutes)
        // Friday 16:00 -> consumes 60 minutes until 17:00 (30 mins remaining)
        // Tuesday 09:00 -> consumes 30 mins -> 09:30 AM!
        LocalDateTime warningTime = slaService.calculateWarningAt(calendar, start, 120);
        assertEquals(LocalDateTime.of(2026, 9, 22, 9, 30), warningTime);
        assertEquals(warningTime, slaCalculationService.calculateWarningAt(calendar, start, 120));
    }
}
