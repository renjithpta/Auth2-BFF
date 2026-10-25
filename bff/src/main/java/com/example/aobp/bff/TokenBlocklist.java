package com.example.aobp.bff;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
/**
 * Denylists the active access token on logout.
 * This does not revoke other sessions or already-rotated tokens.
 * Redis write errors are intentionally propagated (never silently ignored).
 */
@Component public class TokenBlocklist implements LogoutHandler {
 private final StringRedisTemplate redis;
 private final OAuth2AuthorizedClientRepository repository;
 public TokenBlocklist(StringRedisTemplate redis,OAuth2AuthorizedClientRepository repository){this.redis=redis;this.repository=repository;}
 @Override public void logout(HttpServletRequest request,HttpServletResponse response,Authentication authentication){
  if(!(authentication instanceof OAuth2AuthenticationToken oauth))return;
  var authorized=repository.loadAuthorizedClient(oauth.getAuthorizedClientRegistrationId(),oauth,request);
  if(authorized==null||authorized.getAccessToken()==null)return;
  var token=authorized.getAccessToken();
  Duration ttl=Duration.between(Instant.now(),token.getExpiresAt()).plusSeconds(30);
  if(!ttl.isNegative()&&!ttl.isZero()){
   try{
    String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getTokenValue().getBytes(StandardCharsets.UTF_8)));
    redis.opsForValue().set("aobp:blocked:"+hash,"1",ttl);
   }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
  }
  repository.removeAuthorizedClient(oauth.getAuthorizedClientRegistrationId(),oauth,request,response);
 }
}
