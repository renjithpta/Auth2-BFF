package com.example.aobp.bff;

import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/bff/api/v1")
public class RelayController {
  private final DistributedTokenCoordinator tokens;
  private final RestClient client;

  @Value("${services.flight}")
  String flight;

  @Value("${services.workflow}")
  String workflow;

  public RelayController(DistributedTokenCoordinator tokens, RestClient.Builder builder) {
    this.tokens = tokens;
    var f = new JdkClientHttpRequestFactory();
    f.setReadTimeout(java.time.Duration.ofSeconds(3));
    client = builder.requestFactory(f).build();
  }

  @GetMapping("/session")
  Map<String, Object> session(OAuth2AuthenticationToken a) {
    return Map.of(
        "data",
        Map.of(
            "name",
            java.util.Objects.requireNonNullElse(
                a.getPrincipal().getAttribute("preferred_username"), a.getName()),
            "roles",
            a.getAuthorities().stream().map(x -> x.getAuthority()).toList()));
  }

  @GetMapping("/csrf")
  Map<String, String> csrf(CsrfToken t) {
    return Map.of("token", t.getToken(), "headerName", t.getHeaderName());
  }

  @GetMapping("/flights")
  ResponseEntity<String> flights(
      OAuth2AuthenticationToken a, HttpServletRequest q, HttpServletResponse r) {
    return relay(
        flight + "/api/v1/flights" + terminalQuery(q), HttpMethod.GET, null, null, a, q, r);
  }

  @GetMapping("/flights/{id}")
  ResponseEntity<String> get(
      @PathVariable String id,
      OAuth2AuthenticationToken a,
      HttpServletRequest q,
      HttpServletResponse r) {
    return relay(flight + "/api/v1/flights/" + safe(id), HttpMethod.GET, null, null, a, q, r);
  }

  @PatchMapping("/flights/{id}")
  ResponseEntity<String> update(
      @PathVariable String id,
      @RequestBody Map<String, String> body,
      @RequestHeader(value = "If-Match", required = false) String etag,
      OAuth2AuthenticationToken a,
      HttpServletRequest q,
      HttpServletResponse r) {
    return relay(flight + "/api/v1/flights/" + safe(id), HttpMethod.PATCH, body, etag, a, q, r);
  }

  @GetMapping("/workflows")
  ResponseEntity<String> workflows(
      OAuth2AuthenticationToken a, HttpServletRequest q, HttpServletResponse r) {
    return relay(workflow + "/api/v1/workflows", HttpMethod.GET, null, null, a, q, r);
  }

  private String terminalQuery(HttpServletRequest request) {
    String terminal = request.getParameter("terminalId");
    if (terminal == null) return "";
    if (!terminal.matches("[A-Za-z0-9-]{1,16}"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid terminal filter.");
    return "?terminalId=" + terminal;
  }

  private String safe(String id) {
    if (!id.matches("[A-Za-z0-9-]{1,64}"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid identifier.");
    return id;
  }

  private ResponseEntity<String> relay(
      String url,
      HttpMethod method,
      Object body,
      String etag,
      OAuth2AuthenticationToken a,
      HttpServletRequest q,
      HttpServletResponse r) {
    org.springframework.security.oauth2.client.OAuth2AuthorizedClient c;
    try {
      c = tokens.authorized(a, q, r);
    } catch (OAuth2AuthorizationException e) {
      q.getSession().invalidate();
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired. Sign in again.");
    }
    if (c == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again.");
    var spec =
        client
            .method(method)
            .uri(url)
            .headers(
                h -> {
                  h.setBearerAuth(c.getAccessToken().getTokenValue());
                  h.set("X-Request-ID", (String) q.getAttribute("requestId"));
                  if (etag != null) h.set("If-Match", etag);
                });
    if (body != null) spec.contentType(MediaType.APPLICATION_JSON).body(body);
    return spec.exchange(
        (request, response) -> {
          var headers = new HttpHeaders();
          headers.setContentType(
              response.getHeaders().getContentType() == null
                  ? MediaType.APPLICATION_JSON
                  : response.getHeaders().getContentType());
          if (response.getHeaders().getETag() != null)
            headers.setETag(response.getHeaders().getETag());
          return new ResponseEntity<>(
              new String(
                  response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8),
              headers,
              response.getStatusCode());
        });
  }
}
