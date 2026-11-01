package com.example.aobp.bff;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.*;

class RedisAuthorizedClientsTest {
  @AfterEach
  void clearOwner() {
    RedisAuthorizedClients.OWNER.remove();
  }

  @SuppressWarnings("unchecked")
  @Test
  void encryptsTokensAndReloadsFromAuthoritativeStore() {
    var redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    SetOperations<String, String> sets = mock(SetOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(redis.opsForSet()).thenReturn(sets);
    var registration =
        ClientRegistration.withRegistrationId("aobp")
            .clientId("bff")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/callback")
            .authorizationUri("http://localhost/auth")
            .tokenUri("http://localhost/token")
            .build();
    var repo =
        new RedisAuthorizedClients(
            redis,
            new InMemoryClientRegistrationRepository(registration),
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    var token =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "sensitive-access-token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Set.of("openid"));
    var client =
        new OAuth2AuthorizedClient(
            registration,
            "subject",
            token,
            new OAuth2RefreshToken("sensitive-refresh-token", Instant.now()));
    var request = new MockHttpServletRequest();
    var auth = new UsernamePasswordAuthenticationToken("subject", "unused");
    repo.saveAuthorizedClient(client, auth, request, new MockHttpServletResponse());
    var encrypted = ArgumentCaptor.forClass(String.class);
    verify(values)
        .set(
            eq(RedisAuthorizedClients.storage(request, "aobp")),
            encrypted.capture(),
            any(java.time.Duration.class));
    assertThat(encrypted.getValue())
        .doesNotContain("sensitive-access-token", "sensitive-refresh-token");
    when(values.get(RedisAuthorizedClients.storage(request, "aobp")))
        .thenReturn(encrypted.getValue());
    request.changeSessionId();
    OAuth2AuthorizedClient loaded = repo.loadAuthorizedClient("aobp", auth, request);
    assertThat(loaded.getAccessToken().getTokenValue()).isEqualTo("sensitive-access-token");
    assertThat(loaded.getRefreshToken().getTokenValue()).isEqualTo("sensitive-refresh-token");
    RedisAuthorizedClients.OWNER.set("expired-owner");
    when(redis.execute(
            any(org.springframework.data.redis.core.script.RedisScript.class),
            anyList(),
            any(Object[].class)))
        .thenReturn(0L);
    assertThatThrownBy(
            () -> repo.saveAuthorizedClient(client, auth, request, new MockHttpServletResponse()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("could not be saved");
  }
}
