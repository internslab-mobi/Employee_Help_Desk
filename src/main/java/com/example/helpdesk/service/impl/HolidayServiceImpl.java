package com.example.helpdesk.service.impl;

import com.example.helpdesk.config.HolidayConfig;
import com.example.helpdesk.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private final HolidayConfig holidayConfig;

    @Override
    public boolean isHoliday(LocalDate date) {
        return holidayConfig.isHoliday(date);
    }
}
