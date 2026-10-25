package com.example.aobp.flight;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@SpringBootApplication @RestController public class FlightApplication  {
    public static void main(String[] args) {
        SpringApplication.run(FlightApplication.class,args);
    }
    @GetMapping("/flights") public java.util.Map<String,String> list() {
        return java.util.Map.of("service","flight-service","status","ok");
    }
}
