package com.example.aobp.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ResourceSecurity {
  @Bean
  SecurityFilterChain resourceChain(HttpSecurity http, StringRedisTemplate redis) throws Exception {
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(JwtAuthorities::roles);
    http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/actuator/health/**", "/actuator/prometheus")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            o ->
                o.jwt(j -> j.jwtAuthenticationConverter(converter))
                    .authenticationEntryPoint(
                        (q, r, e) -> {
                          r.setHeader("WWW-Authenticate", "Bearer");
                          Problems.write(
                              q,
                              r,
                              401,
                              "AUTHENTICATION_REQUIRED",
                              "A valid access token is required.");
                        }))
        .exceptionHandling(
            e ->
                e.accessDeniedHandler(
                    (q, r, x) ->
                        Problems.write(q, r, 403, "ACCESS_DENIED", "Permission is required.")))
        .addFilterBefore(new RequestLogging(), BearerTokenAuthenticationFilter.class)
        .addFilterAfter(new RevocationCheck(redis), BearerTokenAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  JwtDecoder decoder(
      @Value("${aobp.issuer}") String issuer,
      @Value("${aobp.jwks}") String jwks,
      @Value("${aobp.audience}") String audience) {
    var d = NimbusJwtDecoder.withJwkSetUri(jwks).build();
    OAuth2TokenValidator<Jwt> aud =
        j ->
            j.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Wrong audience", null));
    d.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), aud));
    return d;
  }
}
