package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.holiday.HolidayRequest;
import xyz.mobi.employeehelpdesk.dto.holiday.HolidayResponse;
import xyz.mobi.employeehelpdesk.dto.holiday.HolidayUpdateRequest;

import java.time.LocalDate;
import java.util.List;

public interface HolidayService {

    HolidayResponse createHoliday(HolidayRequest request);

    List<HolidayResponse> getAllHolidays();

    HolidayResponse getHoliday(LocalDate holidayDate);

    HolidayResponse updateHoliday(
            LocalDate holidayDate,
            HolidayUpdateRequest request
    );

    void deleteHoliday(LocalDate holidayDate);
}