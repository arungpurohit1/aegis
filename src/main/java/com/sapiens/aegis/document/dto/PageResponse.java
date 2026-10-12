package com.sapiens.aegis.document.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Wire shape for a page of results. Wrapping Spring Data's Page rather than
 * returning it directly keeps the JSON contract ours to control — Page's own
 * serialized form includes Spring-internal fields (pageable, sort) that
 * callers shouldn't have to depend on.
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages());
    }
}
