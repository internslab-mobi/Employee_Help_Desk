package com.divya.helpdesk.security;

import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.exception.UserNotFoundException;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final HttpServletRequest httpRequest;
    private final JWTService jwtService;
    private final HDEmployeeRepository employeeRepository;

    public Long getEmployeeId() {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtService.extractEmployeeId(token);
        }
        return getCurrentEmployee().getId();
    }

    public HDEmployee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UserNotFoundException("No authenticated user found in security context");
        }
        String email = auth.getName();
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated employee not found with email: " + email));
    }
}