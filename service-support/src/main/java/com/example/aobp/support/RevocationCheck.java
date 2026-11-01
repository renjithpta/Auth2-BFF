package com.example.aobp.support;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class RevocationCheck extends OncePerRequestFilter {
  private final StringRedisTemplate redis;

  public RevocationCheck(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public static String digest(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse r, FilterChain c)
      throws ServletException, java.io.IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken a) {
      try {
        if (Boolean.TRUE.equals(
                redis.hasKey("aobp:blocked:" + digest(a.getToken().getTokenValue())))
            || Boolean.TRUE.equals(
                redis.hasKey("aobp:user:blocked:" + a.getToken().getSubject()))) {
          Problems.write(q, r, 401, "TOKEN_REVOKED", "Access has been revoked.");
          return;
        }
      } catch (DataAccessException e) {
        Problems.write(q, r, 503, "REVOCATION_UNAVAILABLE", "Access verification is unavailable.");
        return;
      }
    }
    c.doFilter(q, r);
  }
}
