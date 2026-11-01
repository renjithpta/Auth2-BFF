package com.example.aobp.flight;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Deliberately in-memory demonstration data; not an operational AODB. */
@Service
public class FlightService {
  public record Flight(
      String flightId,
      String flightNumber,
      String airportId,
      String terminalId,
      String airlineCode,
      String status,
      long version) {}

  private final Map<String, Flight> flights = new ConcurrentHashMap<>();
  private final MeterRegistry meters;
  private final FlightPolicy policy;

  public FlightService(MeterRegistry meters, FlightPolicy policy) {
    this.meters = meters;
    this.policy = policy;
    reset();
  }

  void reset() {
    flights.put(
        "flight-001", new Flight("flight-001", "SN1234", "BRU", "T1", "SN", "SCHEDULED", 1));
    flights.put("flight-002", new Flight("flight-002", "BA456", "BRU", "T2", "BA", "SCHEDULED", 1));
  }

  public List<Flight> list(JwtAuthenticationToken a, String terminalId) {
    policy.require(a, "FLIGHT_VIEW");
    var scope = policy.allowedTerminals(a);
    if (terminalId != null && !scope.contains(terminalId))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Terminal is outside assigned scope.");
    var data =
        flights.values().stream()
            .filter(f -> scope.contains(f.terminalId()))
            .filter(f -> terminalId == null || terminalId.equals(f.terminalId()))
            .sorted(Comparator.comparing(Flight::flightId))
            .toList();
    return data;
  }

  public Flight get(String id, JwtAuthenticationToken principal) {
    return authorized(id, principal);
  }

  private Flight authorized(String id, JwtAuthenticationToken a) {
    policy.require(a, "FLIGHT_VIEW");
    var f = flights.get(id);
    if (f == null || !policy.allowedTerminals(a).contains(f.terminalId()))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight was not found.");
    return f;
  }

  public synchronized Flight update(
      String id, String etag, Map<String, String> body, JwtAuthenticationToken a) {
    var f = authorized(id, a);
    policy.require(a, "FLIGHT_UPDATE");
    if (etag == null)
      throw new ResponseStatusException(HttpStatusCode.valueOf(428), "If-Match is required.");
    if (!etag.equals("\"" + f.version() + "\""))
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "Reload the flight before editing.");
    if (body.size() != 1
        || body.get("status") == null
        || !Set.of("SCHEDULED", "BOARDING", "DEPARTED").contains(body.getOrDefault("status", "")))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a valid status is accepted.");
    if (f.status().equals("DEPARTED"))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Departed flights cannot be changed.");
    var next =
        new Flight(
            f.flightId(),
            f.flightNumber(),
            f.airportId(),
            f.terminalId(),
            f.airlineCode(),
            body.get("status"),
            f.version() + 1);
    flights.put(id, next);
    meters.counter("aobp.flight.updates", "outcome", "success").increment();
    org.slf4j.LoggerFactory.getLogger(getClass())
        .atInfo()
        .addKeyValue("event.name", "flight.updated")
        .addKeyValue("resource.id", id)
        .addKeyValue("actor.id", a.getToken().getSubject())
        .log("Flight updated");
    return next;
  }
}
