package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.UnauthorizedException;
import xyz.mobi.employeehelpdesk.security.UserPrincipal;
import xyz.mobi.employeehelpdesk.service.AuthService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Override
    public Long getCurrentEmployeeId() {
        return getAuthenticatedPrincipal().getEmployeeId();
    }

    @Override
    public UserRole getCurrentUserRole() {
        return getAuthenticatedPrincipal().getRole();
    }

    @Override
    public String getCurrentUserEmail() {
        return getAuthenticatedPrincipal().getEmail();
    }

    @Override
    public String getCurrentUserTimezone() {
        return getAuthenticatedPrincipal().getTimezone();
    }

    @Override
    public java.time.ZoneId getCurrentUserZoneId() {
        return getAuthenticatedPrincipal().getZoneId();
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new UnauthorizedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }

        throw new UnauthorizedException("Invalid authentication principal");
    }
}