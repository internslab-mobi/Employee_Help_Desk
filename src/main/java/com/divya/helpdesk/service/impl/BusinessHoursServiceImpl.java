package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.request.BusinessHoursRequest;
import com.divya.helpdesk.dto.response.BusinessHoursResponse;
import com.divya.helpdesk.entity.HDBusinessCalendar;
import com.divya.helpdesk.entity.HDBusinessHours;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.mapper.BusinessCalendarMapper;
import com.divya.helpdesk.repository.HDBusinessCalendarRepository;
import com.divya.helpdesk.repository.HDBusinessHoursRepository;
import com.divya.helpdesk.service.BusinessHoursService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessHoursServiceImpl implements BusinessHoursService {

    private final HDBusinessHoursRepository businessHoursRepository;
    private final HDBusinessCalendarRepository calendarRepository;
    private final BusinessCalendarMapper calendarMapper;

    @Override
    public BusinessHoursResponse createBusinessHours(Long calendarId, BusinessHoursRequest request) {
        HDBusinessCalendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar not found with id: " + calendarId));

        if (businessHoursRepository.existsByBusinessCalendarIdAndDayOfWeek(calendarId, request.getDayOfWeek())) {
            throw new BadRequestException("Business hours already defined for day " + request.getDayOfWeek() + " in this calendar");
        }

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        HDBusinessHours entity = calendarMapper.toHoursEntity(request);
        entity.setBusinessCalendar(calendar);
        HDBusinessHours saved = businessHoursRepository.save(entity);
        return calendarMapper.toHoursResponse(saved);
    }

    @Override
    public BusinessHoursResponse updateBusinessHours(Long id, BusinessHoursRequest request) {
        HDBusinessHours entity = businessHoursRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business hours not found with id: " + id));

        if (!entity.getDayOfWeek().equals(request.getDayOfWeek()) &&
                businessHoursRepository.existsByBusinessCalendarIdAndDayOfWeek(entity.getBusinessCalendar().getId(), request.getDayOfWeek())) {
            throw new BadRequestException("Business hours already defined for day " + request.getDayOfWeek() + " in this calendar");
        }

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        entity.setDayOfWeek(request.getDayOfWeek());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        if (request.getIsWorkingDay() != null) {
            entity.setIsWorkingDay(request.getIsWorkingDay());
        }

        HDBusinessHours saved = businessHoursRepository.save(entity);
        return calendarMapper.toHoursResponse(saved);
    }

    @Override
    public void deleteBusinessHours(Long id) {
        if (!businessHoursRepository.existsById(id)) {
            throw new ResourceNotFoundException("Business hours not found with id: " + id);
        }
        businessHoursRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessHoursResponse getBusinessHoursById(Long id) {
        HDBusinessHours entity = businessHoursRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business hours not found with id: " + id));
        return calendarMapper.toHoursResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessHoursResponse> getBusinessHoursByCalendarId(Long calendarId) {
        return businessHoursRepository.findByBusinessCalendarId(calendarId).stream()
                .map(calendarMapper::toHoursResponse)
                .collect(Collectors.toList());
    }
}
