package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.exception.HolidayNotFoundException;
import xyz.mobi.employeehelpdesk.entity.Holiday;
import xyz.mobi.employeehelpdesk.repository.HolidayRepository;
import xyz.mobi.employeehelpdesk.service.HolidayService;
import xyz.mobi.employeehelpdesk.dto.holiday.HolidayRequest;
import xyz.mobi.employeehelpdesk.dto.holiday.HolidayResponse;
import xyz.mobi.employeehelpdesk.dto.holiday.HolidayUpdateRequest;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepository;

    @Override
    @Transactional
    public HolidayResponse createHoliday(
            HolidayRequest request) {

        if (holidayRepository.existsByHolidayDate(request.holidayDate())) {
            throw new HolidayNotFoundException(
                    "Holiday already exists for date: " + request.holidayDate()
            );
        }

        Holiday holiday = Holiday.builder()
                .holidayDate(request.holidayDate())
                .holidayName(request.holidayName())
                .build();

        Holiday savedHoliday =
                holidayRepository.save(holiday);

        return toResponse(savedHoliday);
    }

    @Override
    public List<HolidayResponse> getAllHolidays() {

        return holidayRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public HolidayResponse getHoliday(
            LocalDate holidayDate) {

        Holiday holiday =
                holidayRepository.findById(holidayDate)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Holiday not found: "
                                                + holidayDate
                                )
                        );

        return toResponse(holiday);
    }

    @Override
    @Transactional
    public HolidayResponse updateHoliday(
            LocalDate holidayDate,
            HolidayUpdateRequest request) {

        Holiday holiday =
                holidayRepository.findById(holidayDate)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Holiday not found: "
                                                + holidayDate
                                )
                        );

        holiday.setHolidayName(
                request.holidayName()
        );

        return toResponse(holiday);
    }

    @Override
    @Transactional
    public void deleteHoliday(
            LocalDate holidayDate) {

        Holiday holiday =
                holidayRepository.findById(holidayDate)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Holiday not found: "
                                                + holidayDate
                                )
                        );

        holidayRepository.delete(holiday);
    }

    private HolidayResponse toResponse(
            Holiday holiday) {

        return new HolidayResponse(
                holiday.getHolidayDate(),
                holiday.getHolidayName(),
                holiday.getCreatedAt(),
                holiday.getUpdatedAt()
        );
    }
}