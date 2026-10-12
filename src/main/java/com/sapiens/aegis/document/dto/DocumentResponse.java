package com.sapiens.aegis.document.dto;

import java.time.Instant;
import java.util.UUID;

import com.sapiens.aegis.document.Document;

/**
 * Response representation of a document. Keeping the wire format separate from
 * the entity means we can evolve the API — or hide a column — without touching
 * storage, and vice versa.
 */
public record DocumentResponse(
    UUID id,
    String title,
    String content,
    Instant createdAt,
    Instant updatedAt) {

    /** Maps an entity to its outward-facing representation. */
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
            document.getId(),
            document.getTitle(),
            document.getContent(),
            document.getCreatedAt(),
            document.getUpdatedAt());
    }
}
