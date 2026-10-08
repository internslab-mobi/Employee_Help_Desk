package xyz.mobi.employeehelpdesk.service.helperservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import xyz.mobi.employeehelpdesk.entity.ErrorCodeMapping;
import xyz.mobi.employeehelpdesk.repository.ErrorCodeMappingRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorCodeCacheService {

    public static final String CACHE_NAME = "errorCodeMappings";
    private static final String DEFAULT_ERROR_CODE = "ERR_999";

    private final ErrorCodeMappingRepository errorCodeMappingRepository;
    private final CacheManager cacheManager;

    @EventListener(ApplicationReadyEvent.class)
    public void initCache() {
        log.info("Loading error-code mappings into cache at application startup");
        refreshCache();
    }

    public void refreshCache() {
        log.info("Refreshing error-code mappings cache from database");
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
            List<ErrorCodeMapping> mappings = errorCodeMappingRepository.findAll();
            for (ErrorCodeMapping mapping : mappings) {
                if (mapping.getExceptionType() != null && mapping.getErrorCode() != null) {
                    cache.put(mapping.getExceptionType().trim(), mapping.getErrorCode().trim());
                    cache.put(mapping.getExceptionType().toUpperCase().trim(), mapping.getErrorCode().trim());
                }
            }
            log.info("Successfully loaded {} error-code mappings into cache", mappings.size());
        } else {
            log.warn("Cache '{}' not found in CacheManager", CACHE_NAME);
        }
    }

    @Cacheable(
            value = CACHE_NAME,
            key = "#exceptionType"
    )
    public String getErrorCode(String exceptionType) {
        if (exceptionType == null || exceptionType.isBlank()) {
            return DEFAULT_ERROR_CODE;
        }

        String normalizedType = exceptionType.trim();
        log.debug("Cache miss for exceptionType: {}. Querying database.", normalizedType);

        return errorCodeMappingRepository.findByExceptionType(normalizedType)
                .or(() -> errorCodeMappingRepository.findByExceptionType(normalizedType.toUpperCase()))
                .map(ErrorCodeMapping::getErrorCode)
                .orElse(DEFAULT_ERROR_CODE);
    }
}
