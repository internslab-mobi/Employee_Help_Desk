package com.example.helpdesk.service;

import java.time.LocalDateTime;

public interface BusinessTimeService {
    
    LocalDateTime addWorkingMinutes(LocalDateTime from, int minutes);
    
    int calculateWorkingMinutes(LocalDateTime from, LocalDateTime to);
    
    boolean isWorkingDateTime(LocalDateTime dateTime);
    
    LocalDateTime getNextWorkingTime(LocalDateTime dateTime);
}
