package com.example.aobp.bff;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
@Configuration public class SecurityConfig {
 @Bean SecurityFilterChain security(HttpSecurity http,ClientRegistrationRepository registrations) throws Exception {
  var logout=new OidcClientInitiatedLogoutSuccessHandler(registrations);
  logout.setPostLogoutRedirectUri("{baseUrl}/");
  http.authorizeHttpRequests(a->a.requestMatchers("/actuator/health","/api/csrf","/error").permitAll().anyRequest().authenticated())
   .oauth2Login(o->{})
   .oauth2Client(o->{})
   .csrf(c->c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
   .logout(l->l.logoutUrl("/logout").invalidateHttpSession(true).clearAuthentication(true).logoutSuccessHandler(logout));
  return http.build();
 }
}
