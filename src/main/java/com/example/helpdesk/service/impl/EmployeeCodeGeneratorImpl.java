package com.example.helpdesk.service.impl;

import com.example.helpdesk.repository.EmployeeRepository;
import com.example.helpdesk.service.EmployeeCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeCodeGeneratorImpl implements EmployeeCodeGenerator {

    private final EmployeeRepository employeeRepository;

    @Override
    public String generateEmployeeCode(Long employeeId) {
        // Start with the ID-based code
        String employeeCode = String.format("EMP%03d", employeeId);
        
        // Check if this code already exists
        int counter = 1;
        while (employeeRepository.existsByEmployeeCode(employeeCode)) {
            // If exists, append a suffix to make it unique
            employeeCode = String.format("EMP%03d-%d", employeeId, counter);
            counter++;
            
            // Safety check to prevent infinite loop
            if (counter > 1000) {
                throw new IllegalStateException("Cannot generate unique employee code after 1000 attempts");
            }
        }
        
        log.debug("Generated employee code: {} for employee ID: {}", employeeCode, employeeId);
        return employeeCode;
    }
}
