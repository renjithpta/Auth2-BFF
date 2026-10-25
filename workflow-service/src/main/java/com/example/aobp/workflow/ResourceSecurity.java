package com.example.aobp.workflow;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.delegation.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.*;
@Configuration public class ResourceSecurity {
 @Bean SecurityFilterChain chain(HttpSecurity http,RevocationFilter revocation) throws Exception {
  http.csrf(c->c.disable()).authorizeHttpRequests(a->a.requestMatchers("/actuator/health").permitAll().anyRequest().hasAuthority("ROLE_WORKFLOW_VIEW")).oauth2ResourceServer(o->o.jwt(j->{}));
  http.addFilterBefore(revocation,org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class);
  return http.build();
 }
 @Bean JwtDecoder decoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer){
  NimbusJwtDecoder decoder=JwtDecoders.fromIssuerLocation(issuer);
  OAuth2TokenValidator<Jwt> audience=jwt->jwt.getAudience().contains("aobp-api")?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Missing audience",null));
  decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),audience));return decoder;
 }
 @Bean JwtAuthenticationConverter authorities(){
  var c=new JwtAuthenticationConverter();c.setJwtGrantedAuthoritiesConverter(jwt->{
   Map<String,Object> realm=jwt.getClaimAsMap("realm_access");if(realm==null)return List.of();
   Object roles=realm.get("roles");if(!(roles instanceof Collection<?> list))return List.of();
   return list.stream().filter(String.class::isInstance).map(String.class::cast).map(x->new SimpleGrantedAuthority("ROLE_"+x)).toList();
  });return c;
 }
}
