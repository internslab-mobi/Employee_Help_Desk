package com.example.helpdesk.util;

import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedEmployeeUtil {

    private final EmployeeRepository employeeRepository;

    /**
     * Gets the authenticated employee ID from the security context.
     *
     * @return the employee ID, or null if not authenticated
     */
    public Long getAuthenticatedEmployeeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        String principal = authentication.getPrincipal().toString();
        try {
            return Long.parseLong(principal);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets the authenticated employee's timezone.
     * Falls back to UTC if the employee cannot be found or has no timezone.
     *
     * @return the IANA timezone ID (e.g., "Asia/Kolkata"), or "UTC" as fallback
     */
    public String getAuthenticatedEmployeeTimezone() {
        Long employeeId = getAuthenticatedEmployeeId();
        if (employeeId == null) {
            return "UTC";
        }

        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null || employee.getTimezone() == null || employee.getTimezone().trim().isEmpty()) {
            return "UTC";
        }

        return employee.getTimezone();
    }

    /**
     * Gets the authenticated employee entity.
     *
     * @return the employee, or null if not found
     */
    public Employee getAuthenticatedEmployee() {
        Long employeeId = getAuthenticatedEmployeeId();
        if (employeeId == null) {
            return null;
        }
        return employeeRepository.findById(employeeId).orElse(null);
    }
}



