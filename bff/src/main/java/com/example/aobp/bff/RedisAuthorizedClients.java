package com.example.aobp.bff;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.*;
import org.springframework.stereotype.Component;

/** Redis is authoritative; fenced writes reject a refresh worker after lease loss. */
@Component
public class RedisAuthorizedClients implements OAuth2AuthorizedClientRepository {
  private final StringRedisTemplate redis;
  private final ClientRegistrationRepository registrations;
  private final byte[] key;
  private final ObjectMapper json = new ObjectMapper();
  static final ThreadLocal<String> OWNER = new ThreadLocal<>();
  private static final DefaultRedisScript<Long> SAVE =
      new DefaultRedisScript<>(
          "if redis.call('get',KEYS[1]) ~= ARGV[1] then return 0 end redis.call('set',KEYS[2],ARGV[2],'EX',ARGV[3]); return 1",
          Long.class);

  public RedisAuthorizedClients(
      StringRedisTemplate redis,
      ClientRegistrationRepository registrations,
      @Value("${TOKEN_ENCRYPTION_KEY}") String encoded) {
    this.redis = redis;
    this.registrations = registrations;
    this.key = Base64.getDecoder().decode(encoded);
    if (key.length != 32)
      throw new IllegalArgumentException("TOKEN_ENCRYPTION_KEY must encode 32 bytes");
  }

  static String sessionKey(HttpServletRequest request) {
    var session = request.getSession();
    String key = (String) session.getAttribute("AOBP_TOKEN_STORAGE_ID");
    if (key == null) {
      key = UUID.randomUUID().toString();
      session.setAttribute("AOBP_TOKEN_STORAGE_ID", key);
    }
    return key;
  }

  static String storage(HttpServletRequest request, String registration) {
    // Survives Spring Security's session-fixation ID rotation after OAuth login.
    return "aobp:client:" + sessionKey(request) + ":" + registration;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
      String id, Authentication a, HttpServletRequest q) {
    String raw = redis.opsForValue().get(storage(q, id));
    if (raw == null) return null;
    try {
      Map<String, Object> v = json.readValue(crypt(raw, false), Map.class);
      var access =
          new OAuth2AccessToken(
              OAuth2AccessToken.TokenType.BEARER,
              (String) v.get("access"),
              Instant.parse((String) v.get("issued")),
              Instant.parse((String) v.get("expires")),
              new HashSet<>((List<String>) v.get("scopes")));
      OAuth2RefreshToken refresh =
          v.containsKey("refresh")
              ? new OAuth2RefreshToken(
                  (String) v.get("refresh"), Instant.parse((String) v.get("refreshIssued")))
              : null;
      return (T)
          new OAuth2AuthorizedClient(
              registrations.findByRegistrationId(id), (String) v.get("principal"), access, refresh);
    } catch (Exception e) {
      throw new IllegalStateException("Authorized client could not be loaded", e);
    }
  }

  @Override
  public void saveAuthorizedClient(
      OAuth2AuthorizedClient c, Authentication a, HttpServletRequest q, HttpServletResponse r) {
    try {
      var t = c.getAccessToken();
      Map<String, Object> v = new HashMap<>();
      v.put("access", t.getTokenValue());
      v.put("issued", t.getIssuedAt().toString());
      v.put("expires", t.getExpiresAt().toString());
      v.put("scopes", t.getScopes());
      v.put("principal", c.getPrincipalName());
      if (c.getRefreshToken() != null) {
        v.put("refresh", c.getRefreshToken().getTokenValue());
        v.put("refreshIssued", c.getRefreshToken().getIssuedAt().toString());
      }
      String id = c.getClientRegistration().getRegistrationId(),
          k = storage(q, id),
          payload = crypt(json.writeValueAsString(v), true);
      if (OWNER.get() != null) {
        Long saved =
            redis.execute(
                SAVE,
                List.of("aobp:refresh:lock:" + sessionKey(q) + ":" + id, k),
                OWNER.get(),
                payload,
                "3600");
        if (!Long.valueOf(1).equals(saved))
          throw new IllegalStateException("Refresh lease lost; stale write rejected");
      } else redis.opsForValue().set(k, payload, Duration.ofHours(1));
      String registry = "aobp:session-tokens:" + sessionKey(q);
      redis
          .opsForSet()
          .add(registry, com.example.aobp.support.RevocationCheck.digest(t.getTokenValue()));
      redis.expire(registry, Duration.ofHours(1));
    } catch (Exception e) {
      throw new IllegalStateException("Authorized client could not be saved", e);
    }
  }

  @Override
  public void removeAuthorizedClient(
      String id, Authentication a, HttpServletRequest q, HttpServletResponse r) {
    redis.delete(storage(q, id));
  }

  private String crypt(String input, boolean encrypt) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    byte[] nonce = new byte[12];
    byte[] value;
    if (encrypt) {
      new SecureRandom().nextBytes(nonce);
      cipher.init(
          Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
      byte[] bytes = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
      value = new byte[12 + bytes.length];
      System.arraycopy(nonce, 0, value, 0, 12);
      System.arraycopy(bytes, 0, value, 12, bytes.length);
      return Base64.getEncoder().encodeToString(value);
    }
    value = Base64.getDecoder().decode(input);
    System.arraycopy(value, 0, nonce, 0, 12);
    cipher.init(
        Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
    return new String(
        cipher.doFinal(Arrays.copyOfRange(value, 12, value.length)), StandardCharsets.UTF_8);
  }
}
