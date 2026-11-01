package com.example.aobp.bff;

import com.example.aobp.support.*;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
@Import(ApiErrors.class)
public class SecurityConfig {
  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      ClientRegistrationRepository registrations,
      TokenBlocklist blocklist,
      RedisAuthorizedClients clients,
      DefaultOAuth2AuthorizationRequestResolver resolver,
      @org.springframework.beans.factory.annotation.Value("${PUBLIC_ORIGIN:http://localhost:8088}")
          String origin)
      throws Exception {
    var logout = new OidcClientInitiatedLogoutSuccessHandler(registrations);
    logout.setPostLogoutRedirectUri(origin + "/");
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/actuator/health/**",
                        "/actuator/prometheus",
                        "/bff/api/v1/csrf",
                        "/oauth2/**",
                        "/login/**",
                        "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(
            o ->
                o.authorizedClientRepository(clients)
                    .authorizationEndpoint(e -> e.authorizationRequestResolver(resolver))
                    .defaultSuccessUrl(origin + "/", true))
        .oauth2Client(o -> o.authorizedClientRepository(clients))
        .exceptionHandling(
            e ->
                e.defaultAuthenticationEntryPointFor(
                        (q, r, x) ->
                            Problems.write(
                                q, r, 401, "AUTHENTICATION_REQUIRED", "Sign in to continue."),
                        new AntPathRequestMatcher("/bff/**"))
                    .accessDeniedHandler(
                        (q, r, x) ->
                            Problems.write(
                                q,
                                r,
                                403,
                                "ACCESS_DENIED",
                                "Permission or CSRF verification failed.")))
        .logout(
            l ->
                l.logoutUrl("/logout")
                    .addLogoutHandler(blocklist)
                    .deleteCookies("AOBPSESSION")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .logoutSuccessHandler(logout));
    http.addFilterBefore(
        new RequestLogging(),
        org.springframework.security.web.context.SecurityContextHolderFilter.class);
    return http.build();
  }

  @Bean
  GrantedAuthoritiesMapper mapper() {
    return original -> {
      var out = new HashSet<org.springframework.security.core.GrantedAuthority>(original);
      for (var a : original)
        if (a instanceof OidcUserAuthority o) {
          Object realm = o.getIdToken().getClaims().get("realm_access");
          if (realm instanceof Map<?, ?> m && m.get("roles") instanceof Collection<?> roles)
            for (Object role : roles)
              if (role instanceof String s) out.add(new SimpleGrantedAuthority("ROLE_" + s));
        }
      return out;
    };
  }
}
