package com.example.aobp.workflow;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class WorkflowController {
  @GetMapping("/api/v1/workflows")
  public Map<String, Object> list(JwtAuthenticationToken a) {
    if (a.getAuthorities().stream().noneMatch(x -> x.getAuthority().equals("ROLE_WORKFLOW_VIEW")))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Workflow permission is required.");
    return Map.of(
        "data", List.of(Map.of("workflowId", "stand-change-001", "status", "AWAITING_APPROVAL")));
  }
}
