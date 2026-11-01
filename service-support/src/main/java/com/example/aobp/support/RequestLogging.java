package com.example.aobp.support;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.slf4j.*;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestLogging extends OncePerRequestFilter {
  private static final Logger LOG = LoggerFactory.getLogger(RequestLogging.class);

  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse r, FilterChain chain)
      throws ServletException, java.io.IOException {
    String id = q.getHeader("X-Request-ID");
    if (id == null || !id.matches("[A-Za-z0-9_-]{1,80}")) id = UUID.randomUUID().toString();
    q.setAttribute("requestId", id);
    r.setHeader("X-Request-ID", id);
    r.setHeader("Cache-Control", "no-store");
    long start = System.nanoTime();
    try (var ignored = MDC.putCloseable("request.id", id)) {
      chain.doFilter(q, r);
    } finally {
      LOG.atInfo()
          .addKeyValue("event.name", "http.completed")
          .addKeyValue("request.id", id)
          .addKeyValue("http.request.method", q.getMethod())
          .addKeyValue("http.response.status_code", r.getStatus())
          .addKeyValue("duration_ms", (System.nanoTime() - start) / 1000000)
          .log("HTTP request completed");
    }
  }
}
