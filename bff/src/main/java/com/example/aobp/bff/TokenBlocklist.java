package com.example.aobp.bff;

import jakarta.servlet.http.*;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class TokenBlocklist implements LogoutHandler {
  private final StringRedisTemplate redis;
  private final RedisAuthorizedClients clients;

  public TokenBlocklist(StringRedisTemplate redis, RedisAuthorizedClients clients) {
    this.redis = redis;
    this.clients = clients;
  }

  public void logout(HttpServletRequest q, HttpServletResponse r, Authentication a) {
    if (q.getSession(false) == null) return;
    String registry = "aobp:session-tokens:" + RedisAuthorizedClients.sessionKey(q);
    var tokens = redis.opsForSet().members(registry);
    // Realm access-token lifetime is 300s; keep deny records longer than max lifetime + clock skew.
    if (tokens != null)
      for (String hash : tokens)
        redis.opsForValue().set("aobp:blocked:" + hash, "1", Duration.ofSeconds(360));
    if (a instanceof OAuth2AuthenticationToken oauth)
      clients.removeAuthorizedClient(oauth.getAuthorizedClientRegistrationId(), a, q, r);
    redis.delete(registry);
  }
}
