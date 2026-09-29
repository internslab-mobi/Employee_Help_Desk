package com.divya.helpdesk.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Reusable Pagination Response Metadata DTO.
 *
 * Example JSON:
 * {
 *   "content": [...],
 *   "limit": 10,
 *   "offset": 0,
 *   "totalElements": 42,
 *   "hasNext": true
 * }
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private List<T> content;
    private int limit;
    private long offset;
    private long totalElements;
    private boolean hasNext;
}
