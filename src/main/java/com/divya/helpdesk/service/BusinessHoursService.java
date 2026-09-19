package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.request.BusinessHoursRequest;
import com.divya.helpdesk.dto.response.BusinessHoursResponse;

import java.util.List;

public interface BusinessHoursService {
    BusinessHoursResponse createBusinessHours(Long calendarId, BusinessHoursRequest request);
    BusinessHoursResponse updateBusinessHours(Long id, BusinessHoursRequest request);
    void deleteBusinessHours(Long id);
    BusinessHoursResponse getBusinessHoursById(Long id);
    List<BusinessHoursResponse> getBusinessHoursByCalendarId(Long calendarId);
}
