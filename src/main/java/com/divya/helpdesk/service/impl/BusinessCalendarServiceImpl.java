package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.request.BusinessCalendarRequest;
import com.divya.helpdesk.dto.response.BusinessCalendarResponse;
import com.divya.helpdesk.entity.HDBusinessCalendar;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.mapper.BusinessCalendarMapper;
import com.divya.helpdesk.repository.HDBusinessCalendarRepository;
import com.divya.helpdesk.service.BusinessCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessCalendarServiceImpl implements BusinessCalendarService {

    private final HDBusinessCalendarRepository calendarRepository;
    private final BusinessCalendarMapper calendarMapper;

    @Override
    public BusinessCalendarResponse createCalendar(BusinessCalendarRequest request) {
        if (calendarRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Calendar with code '" + request.getCode() + "' already exists");
        }
        HDBusinessCalendar entity = calendarMapper.toEntity(request);
        if (Boolean.TRUE.equals(entity.getIsDefault())) {
            calendarRepository.findByIsDefaultTrue().ifPresent(existingDefault -> {
                existingDefault.setIsDefault(false);
                calendarRepository.save(existingDefault);
            });
        }
        HDBusinessCalendar saved = calendarRepository.save(entity);
        return calendarMapper.toResponse(saved);
    }

    @Override
    public BusinessCalendarResponse updateCalendar(Long id, BusinessCalendarRequest request) {
        HDBusinessCalendar entity = calendarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar not found with id: " + id));

        if (!entity.getCode().equals(request.getCode()) && calendarRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Calendar with code '" + request.getCode() + "' already exists");
        }

        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        if (request.getTimezone() != null) {
            entity.setTimezone(request.getTimezone());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            calendarRepository.findByIsDefaultTrue().ifPresent(existingDefault -> {
                if (!existingDefault.getId().equals(id)) {
                    existingDefault.setIsDefault(false);
                    calendarRepository.save(existingDefault);
                }
            });
            entity.setIsDefault(true);
        } else if (request.getIsDefault() != null) {
            entity.setIsDefault(request.getIsDefault());
        }

        HDBusinessCalendar saved = calendarRepository.save(entity);
        return calendarMapper.toResponse(saved);
    }

    @Override
    public void deleteCalendar(Long id) {
        if (!calendarRepository.existsById(id)) {
            throw new ResourceNotFoundException("Calendar not found with id: " + id);
        }
        calendarRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessCalendarResponse getCalendarById(Long id) {
        HDBusinessCalendar entity = calendarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar not found with id: " + id));
        return calendarMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessCalendarResponse> getAllCalendars() {
        return calendarRepository.findAll().stream()
                .map(calendarMapper::toResponse)
                .collect(Collectors.toList());
    }
}
