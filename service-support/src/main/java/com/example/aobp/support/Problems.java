package com.example.aobp.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.*;

public final class Problems {
  private Problems() {}

  public static ProblemDetail of(int status, String code, String detail, String requestId) {
    var p = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
    p.setType(URI.create("urn:aobp:problem:" + code.toLowerCase().replace('_', '-')));
    p.setInstance(URI.create("urn:uuid:" + UUID.randomUUID()));
    p.setProperty("code", code);
    p.setProperty("requestId", requestId == null ? "unavailable" : requestId);
    String trace = org.slf4j.MDC.get("traceId");
    if (trace != null) p.setProperty("traceId", trace);
    return p;
  }

  public static void write(
      HttpServletRequest q, HttpServletResponse r, int status, String code, String detail)
      throws java.io.IOException {
    r.setStatus(status);
    r.setContentType("application/problem+json");
    new ObjectMapper()
        .writeValue(
            r.getOutputStream(), of(status, code, detail, (String) q.getAttribute("requestId")));
  }
}
