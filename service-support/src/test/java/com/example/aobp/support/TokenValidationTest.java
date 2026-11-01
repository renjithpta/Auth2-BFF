package com.example.aobp.support;

import static org.assertj.core.api.Assertions.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

class TokenValidationTest {
  private String token(RSAKey key, String issuer, String audience, Instant expiry)
      throws Exception {
    var claims =
        new JWTClaimsSet.Builder()
            .subject("system")
            .issuer(issuer)
            .audience(audience)
            .issueTime(Date.from(Instant.now().minusSeconds(600)))
            .expirationTime(Date.from(expiry))
            .build();
    var jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
    jwt.sign(new RSASSASigner(key));
    return jwt.serialize();
  }

  @Test
  void validatesRealSignatureIssuerAudienceAndExpiry() throws Exception {
    var key = new RSAKeyGenerator(2048).keyID("test").generate();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/certs",
        exchange -> {
          byte[] bytes = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (var stream = exchange.getResponseBody()) {
            stream.write(bytes);
          }
        });
    server.start();
    try {
      var decoder =
          new ResourceSecurity()
              .decoder(
                  "https://issuer.example/realms/aobp",
                  "http://127.0.0.1:" + server.getAddress().getPort() + "/certs",
                  "flight-information-api");
      String issuer = "https://issuer.example/realms/aobp";
      assertThat(
              decoder
                  .decode(
                      token(key, issuer, "flight-information-api", Instant.now().plusSeconds(300)))
                  .getSubject())
          .isEqualTo("system");
      String wrongAudience = token(key, issuer, "workflow-api", Instant.now().plusSeconds(300));
      String expired =
          token(key, issuer, "flight-information-api", Instant.now().minusSeconds(300));
      String wrongIssuer =
          token(
              key,
              "https://untrusted.example",
              "flight-information-api",
              Instant.now().plusSeconds(300));
      assertThatThrownBy(() -> decoder.decode(wrongAudience)).isInstanceOf(JwtException.class);
      assertThatThrownBy(() -> decoder.decode(expired)).isInstanceOf(JwtException.class);
      assertThatThrownBy(() -> decoder.decode(wrongIssuer)).isInstanceOf(JwtException.class);
    } finally {
      server.stop(0);
    }
  }
}
