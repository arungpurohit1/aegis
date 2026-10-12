package com.sapiens.aegis.document.dto;

import com.sapiens.aegis.document.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for replacing a document's editable fields (PUT semantics: the
 * client sends the complete new state, so both fields are required).
 *
 * Separate from CreateDocumentRequest even though the shape matches today —
 * create and update drift apart in most real APIs, and sharing one DTO is what
 * makes that painful later.
 */
public record UpdateDocumentRequest(

    @NotBlank(message = "title must not be blank")
    @Size(max = Document.MAX_TITLE_LENGTH, message = "title must be at most 200 characters")
    String title,

    @NotBlank(message = "content must not be blank")
    String content) {
}
