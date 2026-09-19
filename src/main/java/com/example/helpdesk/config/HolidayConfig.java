package com.example.helpdesk.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class HolidayConfig {

    private List<Holiday> holidays = new ArrayList<>();

    @PostConstruct
    public void loadHolidays() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            
            ClassPathResource resource = new ClassPathResource("holidays.json");
            Holiday[] holidayArray = objectMapper.readValue(resource.getInputStream(), Holiday[].class);
            
            holidays = List.of(holidayArray);
            
            System.out.println("Loaded " + holidays.size() + " holidays");
        } catch (IOException e) {
            System.err.println("Failed to load holidays: " + e.getMessage());
        }
    }

    public boolean isHoliday(LocalDate date) {
        return holidays.stream()
                .anyMatch(holiday -> holiday.getDate().equals(date));
    }

    public List<Holiday> getHolidays() {
        return holidays;
    }

    @Data
    public static class Holiday {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private String name;
    }
}
