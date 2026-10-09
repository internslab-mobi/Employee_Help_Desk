package xyz.mobi.employeehelpdesk.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import xyz.mobi.employeehelpdesk.exception.ErrorResponse;
import xyz.mobi.employeehelpdesk.service.helperservice.ErrorCodeCacheService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ErrorCodeCacheService errorCodeCacheService;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        log.warn("Access denied for URI={}: {}", request.getRequestURI(), accessDeniedException != null ? accessDeniedException.getMessage() : "Forbidden");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        String errorCode = "ERR_012";
        if (accessDeniedException != null && errorCodeCacheService != null) {
            String resolved = errorCodeCacheService.getErrorCode(
                    accessDeniedException.getClass().getSimpleName().toUpperCase()
            );
            if (!"ERR_999".equals(resolved)) {
                errorCode = resolved;
            }
        }

        ErrorResponse body = ErrorResponse.of(
                errorCode,
                HttpStatus.FORBIDDEN.value(),
                "You do not have permission to access this resource"
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
