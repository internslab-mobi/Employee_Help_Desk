package com.divya.helpdesk.mapper;

import com.divya.helpdesk.dto.request.BusinessCalendarRequest;
import com.divya.helpdesk.dto.request.BusinessHoursRequest;
import com.divya.helpdesk.dto.request.HolidayRequest;
import com.divya.helpdesk.dto.response.BusinessCalendarResponse;
import com.divya.helpdesk.dto.response.BusinessHoursResponse;
import com.divya.helpdesk.dto.response.HolidayResponse;
import com.divya.helpdesk.entity.HDBusinessCalendar;
import com.divya.helpdesk.entity.HDBusinessHours;
import com.divya.helpdesk.entity.HDHoliday;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class BusinessCalendarMapper {

    public BusinessCalendarResponse toResponse(HDBusinessCalendar entity) {
        if (entity == null) return null;
        BusinessCalendarResponse response = new BusinessCalendarResponse();
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setTimezone(entity.getTimezone());
        response.setIsDefault(entity.getIsDefault());
        response.setIsActive(entity.getIsActive());
        if (entity.getBusinessHours() != null) {
            response.setBusinessHours(entity.getBusinessHours().stream()
                    .map(this::toHoursResponse)
                    .collect(Collectors.toList()));
        }
        if (entity.getHolidays() != null) {
            response.setHolidays(entity.getHolidays().stream()
                    .map(this::toHolidayResponse)
                    .collect(Collectors.toList()));
        }
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public HDBusinessCalendar toEntity(BusinessCalendarRequest request) {
        if (request == null) return null;
        HDBusinessCalendar entity = new HDBusinessCalendar();
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setTimezone(request.getTimezone() != null ? request.getTimezone() : "UTC");
        entity.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return entity;
    }

    public BusinessHoursResponse toHoursResponse(HDBusinessHours entity) {
        if (entity == null) return null;
        BusinessHoursResponse response = new BusinessHoursResponse();
        response.setId(entity.getId());
        if (entity.getBusinessCalendar() != null) {
            response.setBusinessCalendarId(entity.getBusinessCalendar().getId());
        }
        response.setDayOfWeek(entity.getDayOfWeek());
        response.setStartTime(entity.getStartTime());
        response.setEndTime(entity.getEndTime());
        response.setIsWorkingDay(entity.getIsWorkingDay());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public HDBusinessHours toHoursEntity(BusinessHoursRequest request) {
        if (request == null) return null;
        HDBusinessHours entity = new HDBusinessHours();
        entity.setDayOfWeek(request.getDayOfWeek());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setIsWorkingDay(request.getIsWorkingDay() != null ? request.getIsWorkingDay() : true);
        return entity;
    }

    public HolidayResponse toHolidayResponse(HDHoliday entity) {
        if (entity == null) return null;
        HolidayResponse response = new HolidayResponse();
        response.setId(entity.getId());
        if (entity.getBusinessCalendar() != null) {
            response.setBusinessCalendarId(entity.getBusinessCalendar().getId());
        }
        response.setHolidayDate(entity.getHolidayDate());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public HDHoliday toHolidayEntity(HolidayRequest request) {
        if (request == null) return null;
        HDHoliday entity = new HDHoliday();
        entity.setHolidayDate(request.getHolidayDate());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        return entity;
    }
}
