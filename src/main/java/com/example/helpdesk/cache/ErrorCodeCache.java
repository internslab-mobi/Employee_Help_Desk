package com.example.helpdesk.cache;

import com.example.helpdesk.entity.ErrorCode;
import com.example.helpdesk.repository.ErrorCodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ErrorCodeCache {

    private final ErrorCodeRepository errorCodeRepository;
    
    private final ConcurrentHashMap<String, ErrorCode> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadErrorCodes() {
        try {
            log.info("Loading error codes from database into cache...");
            errorCodeRepository.findAll().stream()
                    .filter(ErrorCode::getActive)
                    .forEach(errorCode -> cache.put(errorCode.getCode(), errorCode));
            log.info("Loaded {} error codes into cache", cache.size());
            
            // Ensure ERR_999 exists as fallback
            if (!cache.containsKey("ERR_999")) {
                ErrorCode fallback = ErrorCode.builder()
                        .code("ERR_999")
                        .message("Unknown error / fallback error")
                        .description("Fallback error when requested error code is unavailable")
                        .httpStatus(500)
                        .active(true)
                        .build();
                cache.put("ERR_999", fallback);
                log.warn("ERR_999 not found in database, using in-memory fallback");
            }
        } catch (Exception e) {
            log.error("Failed to load error codes from database, using minimal fallback", e);
            // Minimal in-memory fallback for critical errors
            ErrorCode fallback = ErrorCode.builder()
                    .code("ERR_999")
                    .message("Unknown error / fallback error")
                    .description("Fallback error when requested error code is unavailable")
                    .httpStatus(500)
                    .active(true)
                    .build();
            cache.put("ERR_999", fallback);
        }
    }

    public ErrorCode getErrorCode(String code) {
        ErrorCode errorCode = cache.get(code);
        if (errorCode == null) {
            log.warn("Error code not found in cache: {}, using fallback ERR_999", code);
            return cache.get("ERR_999");
        }
        return errorCode;
    }

    public boolean containsCode(String code) {
        return cache.containsKey(code);
    }

    public int size() {
        return cache.size();
    }
}
