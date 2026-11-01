package com.example.aobp.flight;

import com.example.aobp.support.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ResourceSecurity.class, ApiErrors.class})
public class FlightApplication {
  public static void main(String[] args) {
    SpringApplication.run(FlightApplication.class, args);
  }
}
