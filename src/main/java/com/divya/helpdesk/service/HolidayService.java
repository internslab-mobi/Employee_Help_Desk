package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.request.HolidayRequest;
import com.divya.helpdesk.dto.response.HolidayResponse;

import java.util.List;

public interface HolidayService {
    HolidayResponse createHoliday(Long calendarId, HolidayRequest request);
    HolidayResponse updateHoliday(Long id, HolidayRequest request);
    void deleteHoliday(Long id);
    HolidayResponse getHolidayById(Long id);
    List<HolidayResponse> getHolidaysByCalendarId(Long calendarId);
}
