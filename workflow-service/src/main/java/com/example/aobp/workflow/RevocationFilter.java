package com.example.aobp.workflow;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
@Component public class RevocationFilter extends OncePerRequestFilter {
 private final StringRedisTemplate redis;
 public RevocationFilter(StringRedisTemplate redis){this.redis=redis;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,java.io.IOException{
  String header=request.getHeader("Authorization");if(header!=null&&header.startsWith("Bearer ")){
   try {String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(header.substring(7).getBytes(StandardCharsets.UTF_8)));
    if(Boolean.TRUE.equals(redis.hasKey("aobp:blocked:"+digest))){response.sendError(401,"Revoked token");return;}
   }catch(org.springframework.data.redis.RedisConnectionFailureException e){response.sendError(503,"Revocation store unavailable");return;}
   catch(java.security.NoSuchAlgorithmException e){throw new ServletException(e);}
  }chain.doFilter(request,response);
 }
}
