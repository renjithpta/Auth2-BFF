package com.example.aobp.support;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors
    extends org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler {
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode status,
      org.springframework.web.context.request.WebRequest request) {
    var q = ((org.springframework.web.context.request.ServletWebRequest) request).getRequest();
    return new ResponseEntity<>(
        Problems.of(
            status.value(),
            "INVALID_REQUEST",
            "The request is not valid for this endpoint.",
            (String) q.getAttribute("requestId")),
        headers,
        status);
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  ResponseEntity<ProblemDetail> denied(Exception e, HttpServletRequest q) {
    return response(403, "ACCESS_DENIED", "Permission is required.", q);
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ProblemDetail> expected(ResponseStatusException e, HttpServletRequest q) {
    int s = e.getStatusCode().value();
    String code =
        s == 403
            ? "ACCESS_DENIED"
            : s == 404
                ? "RESOURCE_NOT_FOUND"
                : s == 412 ? "PRECONDITION_FAILED" : "REQUEST_REJECTED";
    return ResponseEntity.status(s)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(Problems.of(s, code, e.getReason(), (String) q.getAttribute("requestId")));
  }

  @ExceptionHandler({
    org.springframework.web.client.ResourceAccessException.class,
    org.springframework.dao.DataAccessException.class
  })
  ResponseEntity<ProblemDetail> unavailable(Exception e, HttpServletRequest q) {
    return response(503, "DEPENDENCY_UNAVAILABLE", "A required service is unavailable.", q);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> unexpected(Exception e, HttpServletRequest q) {
    LoggerFactory.getLogger(getClass()).error("request.failed", e);
    return response(500, "INTERNAL_ERROR", "The request could not be completed.", q);
  }

  private ResponseEntity<ProblemDetail> response(int s, String c, String d, HttpServletRequest q) {
    return ResponseEntity.status(s)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(Problems.of(s, c, d, (String) q.getAttribute("requestId")));
  }
}
