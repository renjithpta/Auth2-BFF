package com.example.aobp.bff;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Service;
/** Coordinates refresh across BFF replicas. Reference: lease expiry still requires fencing. */
@Service public class DistributedTokenCoordinator {
 private final OAuth2AuthorizedClientManager manager;
 private final OAuth2AuthorizedClientRepository repository;
 private final StringRedisTemplate redis;
 private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>("if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end",Long.class);
 public DistributedTokenCoordinator(OAuth2AuthorizedClientManager manager,OAuth2AuthorizedClientRepository repository,StringRedisTemplate redis){this.manager=manager;this.repository=repository;this.redis=redis;}
 public OAuth2AuthorizedClient authorized(OAuth2AuthenticationToken auth,HttpServletRequest request,HttpServletResponse response){
  String registration=auth.getAuthorizedClientRegistrationId();
  OAuth2AuthorizedClient current=repository.loadAuthorizedClient(registration,auth,request);
  if(current==null || current.getAccessToken()==null)return null;
  if(current.getAccessToken().getExpiresAt()!=null && current.getAccessToken().getExpiresAt().isAfter(Instant.now().plusSeconds(90)))return current;
  var session=request.getSession(false);
  if(session==null)return null;
  String key="aobp:refresh:lock:"+session.getId()+":"+registration;
  String owner=UUID.randomUUID().toString();
  for(int attempt=0;attempt<30;attempt++){
   Boolean locked=redis.opsForValue().setIfAbsent(key,owner,Duration.ofSeconds(25));
   if(Boolean.TRUE.equals(locked)){
    try{
     current=repository.loadAuthorizedClient(registration,auth,request);
     if(current==null)return null;
     if(current.getAccessToken().getExpiresAt()!=null && current.getAccessToken().getExpiresAt().isAfter(Instant.now().plusSeconds(90)))return current;
     OAuth2AuthorizeRequest authorization=OAuth2AuthorizeRequest.withClientRegistrationId(registration).principal(auth)
       .attribute(HttpServletRequest.class.getName(),request).attribute(HttpServletResponse.class.getName(),response).build();
     return manager.authorize(authorization);
    } finally {redis.execute(UNLOCK,List.of(key),owner);}
   }
   try{Thread.sleep(100);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Refresh interrupted",e);}
   current=repository.loadAuthorizedClient(registration,auth,request);
   if(current!=null && current.getAccessToken().getExpiresAt()!=null && current.getAccessToken().getExpiresAt().isAfter(Instant.now().plusSeconds(90)))return current;
  }
  throw new IllegalStateException("Refresh coordination timeout");
 }
}