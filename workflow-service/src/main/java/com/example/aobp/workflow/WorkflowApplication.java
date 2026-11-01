package com.example.aobp.workflow;

import com.example.aobp.support.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ResourceSecurity.class, ApiErrors.class})
public class WorkflowApplication {
  public static void main(String[] args) {
    SpringApplication.run(WorkflowApplication.class, args);
  }
}
