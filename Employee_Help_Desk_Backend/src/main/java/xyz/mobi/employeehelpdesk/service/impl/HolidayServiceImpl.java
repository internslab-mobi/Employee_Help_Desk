package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.holiday.CreateHolidayRequestDTO;
import xyz.mobi.employeehelpdesk.dto.holiday.HolidayResponseDTO;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.Holiday;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.DuplicateResourceException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.DepartmentRepository;
import xyz.mobi.employeehelpdesk.repository.HolidayRepository;
import xyz.mobi.employeehelpdesk.service.HolidayService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public List<HolidayResponseDTO> getHolidays(Long departmentId, Integer year) {
        log.debug("Fetching holidays: departmentId={}, year={}", departmentId, year);
        List<Holiday> holidays;

        if (departmentId != null && year != null) {
            LocalDate from = LocalDate.of(year, 1, 1);
            LocalDate to = LocalDate.of(year, 12, 31);
            holidays = holidayRepository.findByDepartmentIdAndHolidayDateBetween(departmentId, from, to);
        } else if (departmentId != null) {
            holidays = holidayRepository.findByDepartmentIdOrderByHolidayDateAsc(departmentId);
        } else {
            holidays = holidayRepository.findAllByOrderByHolidayDateAsc();
        }

        return holidays.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public HolidayResponseDTO createHoliday(CreateHolidayRequestDTO request) {
        log.info("Creating new holiday: deptId={}, date={}, name={}",
                request.departmentId(), request.holidayDate(), request.holidayName());

        Department department = departmentRepository.findByIdAndIsActiveTrue(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));

        if (holidayRepository.existsByDepartmentIdAndHolidayDate(department.getId(), request.holidayDate())) {
            throw new DuplicateResourceException("Holiday on " + request.holidayDate() + " already exists for department " + department.getName());
        }

        Holiday holiday = Holiday.builder()
                .department(department)
                .holidayDate(request.holidayDate())
                .holidayName(request.holidayName().trim())
                .build();

        Holiday saved = holidayRepository.save(holiday);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteHoliday(Long id) {
        log.info("Deleting holiday id={}", id);
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));
        holidayRepository.delete(holiday);
    }

    private HolidayResponseDTO toResponse(Holiday holiday) {
        Department dept = holiday.getDepartment();
        return HolidayResponseDTO.builder()
                .id(holiday.getId())
                .departmentId(dept != null ? dept.getId() : null)
                .departmentName(dept != null ? dept.getName() : null)
                .holidayDate(holiday.getHolidayDate())
                .holidayName(holiday.getHolidayName())
                .build();
    }
}
