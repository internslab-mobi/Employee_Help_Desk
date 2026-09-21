package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.request.HolidayRequest;
import com.divya.helpdesk.dto.response.HolidayResponse;
import com.divya.helpdesk.entity.HDBusinessCalendar;
import com.divya.helpdesk.entity.HDHoliday;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.mapper.BusinessCalendarMapper;
import com.divya.helpdesk.repository.HDBusinessCalendarRepository;
import com.divya.helpdesk.repository.HDHolidayRepository;
import com.divya.helpdesk.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HolidayServiceImpl implements HolidayService {

    private final HDHolidayRepository holidayRepository;
    private final HDBusinessCalendarRepository calendarRepository;
    private final BusinessCalendarMapper calendarMapper;

    @Override
    public HolidayResponse createHoliday(Long calendarId, HolidayRequest request) {
        HDBusinessCalendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar not found with id: " + calendarId));

        if (holidayRepository.existsByBusinessCalendarIdAndHolidayDate(calendarId, request.getHolidayDate())) {
            throw new BadRequestException("Holiday already defined for date " + request.getHolidayDate() + " in this calendar");
        }

        HDHoliday entity = calendarMapper.toHolidayEntity(request);
        entity.setBusinessCalendar(calendar);
        HDHoliday saved = holidayRepository.save(entity);
        return calendarMapper.toHolidayResponse(saved);
    }

    @Override
    public HolidayResponse updateHoliday(Long id, HolidayRequest request) {
        HDHoliday entity = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));

        if (!entity.getHolidayDate().equals(request.getHolidayDate()) &&
                holidayRepository.existsByBusinessCalendarIdAndHolidayDate(entity.getBusinessCalendar().getId(), request.getHolidayDate())) {
            throw new BadRequestException("Holiday already defined for date " + request.getHolidayDate() + " in this calendar");
        }

        entity.setHolidayDate(request.getHolidayDate());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());

        HDHoliday saved = holidayRepository.save(entity);
        return calendarMapper.toHolidayResponse(saved);
    }

    @Override
    public void deleteHoliday(Long id) {
        if (!holidayRepository.existsById(id)) {
            throw new ResourceNotFoundException("Holiday not found with id: " + id);
        }
        holidayRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public HolidayResponse getHolidayById(Long id) {
        HDHoliday entity = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));
        return calendarMapper.toHolidayResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> getHolidaysByCalendarId(Long calendarId) {
        return holidayRepository.findByBusinessCalendarId(calendarId).stream()
                .map(calendarMapper::toHolidayResponse)
                .collect(Collectors.toList());
    }
}
