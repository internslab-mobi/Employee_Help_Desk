package xyz.mobi.employeehelpdesk.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.ErrorResponse;
import xyz.mobi.employeehelpdesk.service.helperservice.ErrorCodeCacheService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final ErrorCodeCacheService errorCodeCacheService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = getJwtFromRequest(request);

        if (StringUtils.hasText(jwt)) {
            // Token was provided — it MUST be valid
            if (!tokenProvider.validateToken(jwt)) {
                writeUnauthorizedResponse(response, "Invalid or expired token");
                return;
            }

            try {
                Long employeeId = tokenProvider.getEmployeeIdFromToken(jwt);
                UserRole role = tokenProvider.getRoleFromToken(jwt);
                String timezone = tokenProvider.getTimezoneFromToken(jwt);

                // Validate timezone if present; fallback to UTC for backward compatibility with old tokens
                if (timezone != null && !timezone.isBlank()) {
                    try {
                        java.time.ZoneId.of(timezone.trim());
                        timezone = timezone.trim();
                    } catch (Exception ex) {
                        log.error("Invalid timezone claim in JWT: {}", timezone);
                        writeUnauthorizedResponse(response, "Invalid token");
                        return;
                    }
                } else {
                    timezone = "UTC";
                }

                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role.name())
                );

                UserPrincipal principal = new UserPrincipal(
                        employeeId,
                        null,
                        null,
                        role,
                        timezone,
                        true,
                        authorities
                );

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                log.error("Failed to extract user details from JWT: {}", ex.getMessage());
                writeUnauthorizedResponse(response, "Invalid token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String errorCode = "ERR_018";
        if (errorCodeCacheService != null) {
            String resolved = errorCodeCacheService.getErrorCode("JWTEXCEPTION");
            if (!"ERR_999".equals(resolved)) {
                errorCode = resolved;
            }
        }

        ErrorResponse body = ErrorResponse.of(
                errorCode,
                HttpStatus.UNAUTHORIZED.value(),
                message
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
