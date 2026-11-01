package com.example.aobp.flight;

import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Policy enforcement uses only verified token claims and service-owned resource attributes. */
@Component
public class FlightPolicy {
  public Set<String> allowedTerminals(JwtAuthenticationToken principal) {
    List<String> values = principal.getToken().getClaimAsStringList("terminals");
    return values == null ? Set.of() : Set.copyOf(values);
  }

  public void require(JwtAuthenticationToken principal, String role) {
    boolean allowed =
        principal.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    if (!allowed)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission is required.");
  }
}
