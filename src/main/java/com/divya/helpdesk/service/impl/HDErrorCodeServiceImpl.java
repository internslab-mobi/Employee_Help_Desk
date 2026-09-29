package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.entity.HDErrorCode;
import com.divya.helpdesk.repository.HDErrorCodeRepository;
import com.divya.helpdesk.service.HDErrorCodeService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class HDErrorCodeServiceImpl implements HDErrorCodeService {

    private final HDErrorCodeRepository errorCodeRepository;

    // In-memory cache for Exception Type -> Error Code
    private final Map<String, String> exceptionToErrorCodeMap = new ConcurrentHashMap<>();
    // In-memory cache for Error Code -> Default Message
    private final Map<String, String> errorCodeToMessageMap = new ConcurrentHashMap<>();

    @Override
    @Cacheable(value = "errorCodes")
    public Map<String, String> loadAllErrorCodes() {
        try {
            var dbCodes = errorCodeRepository.findAll();
            for (HDErrorCode ec : dbCodes) {
                if (ec.getExceptionType() != null && ec.getErrorCode() != null) {
                    exceptionToErrorCodeMap.put(ec.getExceptionType().toUpperCase(), ec.getErrorCode());
                    errorCodeToMessageMap.put(ec.getErrorCode(), ec.getExceptionType().toUpperCase());
                }
            }
        } catch (Exception e) {
            log.warn("Could not load error codes from database, using default in-memory cache: {}", e.getMessage());
        }

        return exceptionToErrorCodeMap;
    }

    @Override
    public String getErrorCode(String exceptionType) {
        if (exceptionType == null) {
            return "ERR_999";
        }
        return exceptionToErrorCodeMap.getOrDefault(exceptionType.toUpperCase(), "ERR_999");
    }

    @Override
    public String getErrorMessage(String errorCode) {
        if (errorCode == null) {
            return "An unexpected error occurred";
        }
        return errorCodeToMessageMap.getOrDefault(errorCode, "An unexpected error occurred");
    }
}

