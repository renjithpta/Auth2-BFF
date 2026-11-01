package com.example.aobp.support;

import java.util.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtAuthorities {
  private JwtAuthorities() {}

  public static Collection<GrantedAuthority> roles(Jwt jwt) {
    Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
    if (realm == null || !(realm.get("roles") instanceof Collection<?> roles)) return List.of();
    return roles.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
        .toList();
  }
}
