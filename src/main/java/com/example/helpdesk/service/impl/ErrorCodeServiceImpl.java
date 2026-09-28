package com.example.helpdesk.service.impl;

import com.example.helpdesk.entity.ErrorCode;
import com.example.helpdesk.repository.ErrorCodeRepository;
import com.example.helpdesk.service.ErrorCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorCodeServiceImpl implements ErrorCodeService {

    private final ErrorCodeRepository errorCodeRepository;

    @Override
    public Optional<ErrorCode> findByCode(String code) {
        try {
            return errorCodeRepository.findByCodeAndActiveTrue(code);
        } catch (Exception e) {
            log.error("Error fetching error code: {}", code, e);
            return Optional.empty();
        }
    }

    @Override
    public ErrorCode getByCode(String code) {
        Optional<ErrorCode> errorCodeOpt = findByCode(code);
        
        if (errorCodeOpt.isPresent()) {
            return errorCodeOpt.get();
        }
        
        // Fallback to a default error code if not found or database error
        log.warn("Error code not found or database error, using fallback: {}", code);
        return ErrorCode.builder()
                .code(code)
                .message("An unexpected error occurred")
                .httpStatus(500)
                .active(true)
                .build();
    }
}
