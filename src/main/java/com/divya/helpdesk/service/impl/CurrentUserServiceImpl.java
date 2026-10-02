package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.entity.HDEmployeeEntity;
import com.divya.helpdesk.exception.UserNotFoundException;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import com.divya.helpdesk.service.CurrentUserService;
import com.divya.helpdesk.service.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private final HttpServletRequest httpRequest;
    private final JWTService jwtService;
    private final HDEmployeeRepository employeeRepository;

    @Override
    public Long getEmployeeId() {
        return getCurrentEmployee().getId();
    }

    @Override
    public HDEmployeeEntity getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UserNotFoundException("No authenticated user found in security context");
        }
        String email = auth.getName();
        HDEmployeeEntity employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated employee not found with email: " + email));

        if (!Boolean.TRUE.equals(employee.getActivated())) {
            throw new com.divya.helpdesk.exception.AccountNotActivatedException(
                    "Account is not activated. Password change is required before accessing the application.");
        }

        return employee;
    }
}
