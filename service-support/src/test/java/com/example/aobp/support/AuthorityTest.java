package com.example.aobp.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AuthorityTest {
  @Test
  void noRolesMeansNoAuthority() {
    assertThat(
            JwtAuthorities.roles(
                Jwt.withTokenValue("x").header("alg", "RS256").subject("s").build()))
        .isEmpty();
  }

  @Test
  void mapsOnlyStringRoles() {
    var j =
        Jwt.withTokenValue("x")
            .header("alg", "RS256")
            .claim("realm_access", Map.of("roles", List.of("FLIGHT_VIEW", 5)))
            .build();
    assertThat(JwtAuthorities.roles(j)).extracting("authority").containsExactly("ROLE_FLIGHT_VIEW");
  }
}
