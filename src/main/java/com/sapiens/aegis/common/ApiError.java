package com.sapiens.aegis.common;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The single error shape every failing request returns, so clients only ever have
 * to parse one thing. Null fields are omitted, so a plain 404 stays compact while
 * a validation failure can carry per-field detail.
 *
 * @param correlationId lets a user quote one id that also appears in our logs
 * @param fieldErrors   field name -> what was wrong (validation failures only)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String correlationId,
    Map<String, String> fieldErrors) {
}
