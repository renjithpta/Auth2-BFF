package com.example.aobp.bff;

import java.time.Duration;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;

@Configuration
public class OAuthClientConfig {
  @Bean
  ClientRegistrationRepository registrations(
      @org.springframework.beans.factory.annotation.Value(
              "${OIDC_ISSUER_URI:http://localhost:8085/realms/aobp}")
          String issuer,
      @org.springframework.beans.factory.annotation.Value(
              "${OIDC_INTERNAL_BASE:http://localhost:8085}")
          String internal,
      @org.springframework.beans.factory.annotation.Value("${PUBLIC_ORIGIN:http://localhost:8088}")
          String origin,
      @org.springframework.beans.factory.annotation.Value("${OIDC_CLIENT_SECRET}") String secret) {
    String base = internal + "/realms/aobp/protocol/openid-connect";
    var registration =
        org.springframework.security.oauth2.client.registration.ClientRegistration
            .withRegistrationId("aobp")
            .clientId("aobp-bff")
            .clientSecret(secret)
            .clientAuthenticationMethod(
                org.springframework.security.oauth2.core.ClientAuthenticationMethod
                    .CLIENT_SECRET_BASIC)
            .authorizationGrantType(
                org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(origin + "/login/oauth2/code/aobp")
            .scope("openid", "profile", "email")
            .authorizationUri(issuer + "/protocol/openid-connect/auth")
            .tokenUri(base + "/token")
            .jwkSetUri(base + "/certs")
            .userInfoUri(base + "/userinfo")
            .userNameAttributeName("sub")
            .issuerUri(issuer)
            .providerConfigurationMetadata(
                java.util.Map.of(
                    "end_session_endpoint", issuer + "/protocol/openid-connect/logout"))
            .build();
    return new org.springframework.security.oauth2.client.registration
        .InMemoryClientRegistrationRepository(registration);
  }

  @Bean
  OAuth2AuthorizedClientManager manager(
      ClientRegistrationRepository registrations, RedisAuthorizedClients repository) {
    var m = new DefaultOAuth2AuthorizedClientManager(registrations, repository);
    var refresh = new RefreshTokenOAuth2AuthorizedClientProvider();
    refresh.setClockSkew(Duration.ofSeconds(90));
    m.setAuthorizedClientProvider(refresh);
    return m;
  }

  @Bean
  DefaultOAuth2AuthorizationRequestResolver resolver(ClientRegistrationRepository registrations) {
    var r = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
    r.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
    return r;
  }
}
