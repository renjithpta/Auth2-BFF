package com.example.aobp.bff;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;


import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
@RestController public class RelayController {
 private final DistributedTokenCoordinator coordinator;
 private final RestClient client=RestClient.create();
 @Value("${services.flight:http://localhost:8091}") String flight;
 @Value("${services.workflow:http://localhost:8092}") String workflow;
 public RelayController(DistributedTokenCoordinator coordinator){this.coordinator=coordinator;}
 @GetMapping("/api/session") public Map<String,Object> session(Authentication auth){return Map.of("authenticated",true,"name",auth.getName());}
 @GetMapping("/api/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @GetMapping("/api/flights") public ResponseEntity<String> flights(OAuth2AuthenticationToken auth,HttpServletRequest req,HttpServletResponse res){return relay(flight+"/flights",auth,req,res);}
 @GetMapping("/api/workflows") public ResponseEntity<String> workflows(OAuth2AuthenticationToken auth,HttpServletRequest req,HttpServletResponse res){return relay(workflow+"/workflows",auth,req,res);}
 private ResponseEntity<String> relay(String url,OAuth2AuthenticationToken auth,HttpServletRequest req,HttpServletResponse res){
  var authorized=coordinator.authorized(auth,req,res);
  if(authorized==null||authorized.getAccessToken()==null)return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  return client.get().uri(URI.create(url)).headers(h->h.setBearerAuth(authorized.getAccessToken().getTokenValue())).exchange((r,s)->ResponseEntity.status(s.getStatusCode()).body(new String(s.getBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)));
 }
}
