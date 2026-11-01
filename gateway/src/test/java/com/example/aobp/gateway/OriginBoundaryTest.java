package com.example.aobp.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class OriginBoundaryTest {
  @Test
  void rejectsForeignOriginBeforeWebSocketUpgrade() {
    var exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/bff/ws/v1").header("Origin", "http://untrusted.example"));
    new GatewaySecurity()
        .websocketOrigin("http://localhost:8088")
        .filter(
            exchange,
            next -> {
              throw new AssertionError("Untrusted origin must not reach upstream");
            })
        .block();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void permitsConfiguredOriginToReachBffSessionBoundary() {
    var exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/bff/ws/v1").header("Origin", "http://localhost:8088"));
    new GatewaySecurity()
        .websocketOrigin("http://localhost:8088")
        .filter(
            exchange,
            next -> {
              next.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
              return Mono.empty();
            })
        .block();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }
}
