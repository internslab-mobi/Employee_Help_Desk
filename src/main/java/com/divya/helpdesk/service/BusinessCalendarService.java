package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.request.BusinessCalendarRequest;
import com.divya.helpdesk.dto.response.BusinessCalendarResponse;

import java.util.List;

public interface BusinessCalendarService {
    BusinessCalendarResponse createCalendar(BusinessCalendarRequest request);
    BusinessCalendarResponse updateCalendar(Long id, BusinessCalendarRequest request);
    void deleteCalendar(Long id);
    BusinessCalendarResponse getCalendarById(Long id);
    List<BusinessCalendarResponse> getAllCalendars();
}
