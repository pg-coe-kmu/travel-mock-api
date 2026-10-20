package com.coe.b04.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Zentrale Fehler-Antworten fuer die API:
 *  - @Validated + @NotBlank auf Handler-Parametern laeuft ueber den
 *    Methoden-Validierungs-Proxy und wirft ConstraintViolationException,
 *    die Spring sonst auf 500 mappt. Hier: 400 mit erster Meldung.
 *  - ResponseStatusException (4xx/503 aus den Services) wird mit dem
 *    handgeschriebenen Grund (reason) beantwortet - NUR dieser Text wird
 *    exponiert, keine internen Exception-/SQL-Messages. Deshalb bleibt
 *    server.error.include-message auf dem Boot-Default (never).
 */
@ControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException e,
                                                                    HttpServletRequest request) {
        HttpStatusCode code = e.getStatusCode();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", code.value());
        body.put("error", code instanceof HttpStatus hs ? hs.getReasonPhrase() : code.toString());
        body.put("message", e.getReason() != null ? e.getReason() : "Request failed");
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(code).body(body);
    }
}
