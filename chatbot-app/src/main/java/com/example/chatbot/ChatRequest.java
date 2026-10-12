package com.example.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Incoming chat message from the browser.
 *
 * Input validation control: length and blankness are enforced here so a
 * malformed or oversized payload is rejected before it ever reaches the
 * LLM call or any downstream processing.
 */
public record ChatRequest(
    @NotBlank(message = "Message must not be empty")
    @Size(max = 4000, message = "Message must be 4000 characters or fewer")
    String message
) {
}
