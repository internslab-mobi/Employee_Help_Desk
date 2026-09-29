package xyz.mobi.employeehelpdesk.service.helperservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import xyz.mobi.employeehelpdesk.entity.ErrorCodeMapping;
import xyz.mobi.employeehelpdesk.repository.ErrorCodeMappingRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorCodeCacheService {

    private static final String DEFAULT_ERROR_CODE = "ERR_999";

    private final ErrorCodeMappingRepository errorCodeMappingRepository;

    @Cacheable(
            value = "errorCodeMappings",
            key = "#exceptionType"
    )
    public String getErrorCode(String exceptionType) {
        if (exceptionType == null || exceptionType.isBlank()) {
            return DEFAULT_ERROR_CODE;
        }

        log.debug("Cache miss for exceptionType: {}. Querying database.", exceptionType);

        return errorCodeMappingRepository.findByExceptionType(exceptionType)
                .map(ErrorCodeMapping::getErrorCode)
                .orElse(DEFAULT_ERROR_CODE);
    }
}
