package com.example.aobp.workflow;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@SpringBootApplication @RestController public class WorkflowApplication  {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class,args);
    }
    @GetMapping("/workflows") public java.util.Map<String,String> list() {
        return java.util.Map.of("service","workflow-service","status","ok");
    }
}
