package com.sapiens.aegis.document;

import java.util.UUID;

/**
 * Thrown when a requested document id does not exist.
 * ApiExceptionHandler maps it to HTTP 404, so the service can simply throw and
 * the controller needs no null checks.
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(UUID id) {
        super("Document not found: " + id);
    }
}
