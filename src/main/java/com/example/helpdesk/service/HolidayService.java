package com.example.helpdesk.service;

import java.time.LocalDate;

public interface HolidayService {
    
    boolean isHoliday(LocalDate date);
}
