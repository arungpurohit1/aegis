package com.sapiens.aegis.document.dto;

import com.sapiens.aegis.document.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a document.
 *
 * The annotations are Jakarta Bean Validation: with @Valid on the controller
 * parameter, Spring checks them before our code runs and a bad request never
 * reaches the service. Failures surface as a 400 with per-field messages
 * (see ApiExceptionHandler).
 */
public record CreateDocumentRequest(

    @NotBlank(message = "title must not be blank")
    @Size(max = Document.MAX_TITLE_LENGTH, message = "title must be at most 200 characters")
    String title,

    @NotBlank(message = "content must not be blank")
    String content) {
}
