package com.example.aobp.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
public class GatewaySecurity {
  @Bean
  @org.springframework.core.annotation.Order(-200)
  org.springframework.web.server.WebFilter websocketOrigin(
      @Value("${PUBLIC_ORIGIN:http://localhost:8088}") String origin) {
    return (exchange, chain) -> {
      if (exchange.getRequest().getPath().value().equals("/bff/ws/v1")
          && !origin.equals(exchange.getRequest().getHeaders().getOrigin())) {
        return problem(exchange, 403, "ORIGIN_DENIED");
      }
      return chain.filter(exchange);
    };
  }

  @Bean
  SecurityWebFilterChain edge(ServerHttpSecurity http) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .logout(ServerHttpSecurity.LogoutSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .securityContextRepository(
            org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
                .getInstance())
        .authorizeExchange(
            a -> a.pathMatchers("/api/v1/flights/**").authenticated().anyExchange().permitAll())
        .oauth2ResourceServer(
            o ->
                o.jwt(j -> {})
                    .authenticationEntryPoint((e, x) -> problem(e, 401, "AUTHENTICATION_REQUIRED")))
        .exceptionHandling(e -> e.accessDeniedHandler((x, y) -> problem(x, 403, "ACCESS_DENIED")))
        .build();
  }

  static Mono<Void> problem(
      org.springframework.web.server.ServerWebExchange e, int status, String code) {
    var r = e.getResponse();
    r.setStatusCode(org.springframework.http.HttpStatusCode.valueOf(status));
    r.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    if (status == 401) r.getHeaders().set("WWW-Authenticate", "Bearer");
    return r.writeWith(
        Mono.just(
            r.bufferFactory()
                .wrap(
                    ("{\"type\":\"urn:aobp:problem:"
                            + code.toLowerCase()
                            + "\",\"title\":\"Access rejected\",\"status\":"
                            + status
                            + ",\"code\":\""
                            + code
                            + "\"}")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))));
  }

  @Bean
  ReactiveJwtDecoder jwt(
      @Value("${aobp.issuer}") String issuer, @Value("${aobp.jwks}") String jwks) {
    var d = NimbusReactiveJwtDecoder.withJwkSetUri(jwks).build();
    OAuth2TokenValidator<Jwt> aud =
        j ->
            j.getAudience().contains("flight-information-api")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    d.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), aud));
    return d;
  }
}
