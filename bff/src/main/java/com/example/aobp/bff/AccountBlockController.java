package com.example.aobp.bff;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin") public class AccountBlockController {
 private final StringRedisTemplate redis;
 public AccountBlockController(StringRedisTemplate redis){this.redis=redis;}
 @PostMapping("/blocked-users/{subject}") @PreAuthorize("hasAuthority('ROLE_AOBP_ADMIN')")
 public Map<String,Object> block(@PathVariable String subject){redis.opsForValue().set("aobp:user:blocked:"+subject,"1");return Map.of("blocked",true);}
 @DeleteMapping("/blocked-users/{subject}") @PreAuthorize("hasAuthority('ROLE_AOBP_ADMIN')")
 public Map<String,Object> unblock(@PathVariable String subject){redis.delete("aobp:user:blocked:"+subject);return Map.of("blocked",false);}
}