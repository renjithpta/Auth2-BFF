package com.example.aobp.flight;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

/** HTTP adapter; use cases and access policies are separate collaborators. */
@RestController
@RequestMapping("/api/v1/flights")
public class FlightController {
  private final FlightService flights;

  public FlightController(FlightService flights) {
    this.flights = flights;
  }

  @GetMapping
  public Map<String, Object> list(
      JwtAuthenticationToken principal, @RequestParam(required = false) String terminalId) {
    var data = flights.list(principal, terminalId);
    return Map.of("data", data, "meta", Map.of("count", data.size()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Map<String, FlightService.Flight>> get(
      @PathVariable String id, JwtAuthenticationToken principal) {
    return response(flights.get(id, principal));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Map<String, FlightService.Flight>> update(
      @PathVariable String id,
      @RequestHeader(value = "If-Match", required = false) String etag,
      @RequestBody Map<String, String> body,
      JwtAuthenticationToken principal) {
    return response(flights.update(id, etag, body, principal));
  }

  private ResponseEntity<Map<String, FlightService.Flight>> response(FlightService.Flight flight) {
    return ResponseEntity.ok().eTag("\"" + flight.version() + "\"").body(Map.of("data", flight));
  }
}
