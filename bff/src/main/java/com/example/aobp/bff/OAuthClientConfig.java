package com.example.aobp.bff;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
@Configuration public class OAuthClientConfig {
 @Bean OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registrations,OAuth2AuthorizedClientRepository repository){
  var manager=new DefaultOAuth2AuthorizedClientManager(registrations,repository);
  manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().authorizationCode().refreshToken().build());
  return manager;
 }
}
