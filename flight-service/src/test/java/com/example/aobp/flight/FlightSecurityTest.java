package com.example.aobp.flight;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "aobp.issuer=http://issuer/realms/aobp",
      "aobp.jwks=http://issuer/certs",
      "management.server.port=0"
    })
@AutoConfigureMockMvc
class FlightSecurityTest {
  @Autowired MockMvc mvc;
  @Autowired FlightService controller;
  @MockitoBean JwtDecoder decoder;
  @MockitoBean StringRedisTemplate redis;

  @BeforeEach
  void setup() {
    controller.reset();
    reset(redis);
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor viewer() {
    return jwt()
        .jwt(j -> j.subject("ops").claim("terminals", List.of("T1")))
        .authorities(new SimpleGrantedAuthority("ROLE_FLIGHT_VIEW"));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor operator() {
    return jwt()
        .jwt(j -> j.subject("ops").claim("terminals", List.of("T1")))
        .authorities(
            new SimpleGrantedAuthority("ROLE_FLIGHT_VIEW"),
            new SimpleGrantedAuthority("ROLE_FLIGHT_UPDATE"));
  }

  @Test
  void anonymousIs401() throws Exception {
    mvc.perform(get("/api/v1/flights"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }

  @Test
  void listsOnlyAuthorizedTerminal() throws Exception {
    mvc.perform(get("/api/v1/flights").with(viewer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].terminalId").value("T1"));
  }

  @Test
  void deniesForeignFilter() throws Exception {
    mvc.perform(get("/api/v1/flights?terminalId=T2").with(viewer()))
        .andExpect(status().isForbidden());
  }

  @Test
  void concealsForeignObject() throws Exception {
    mvc.perform(get("/api/v1/flights/flight-002").with(viewer())).andExpect(status().isNotFound());
  }

  @Test
  void missingRoleDenied() throws Exception {
    mvc.perform(get("/api/v1/flights").with(jwt())).andExpect(status().isForbidden());
  }

  @Test
  void readOnlyCannotMutate() throws Exception {
    mvc.perform(
            patch("/api/v1/flights/flight-001")
                .with(viewer())
                .header("If-Match", "\"1\"")
                .contentType("application/json")
                .content("{\"status\":\"BOARDING\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void conditionalUpdateAndStaleWrite() throws Exception {
    mvc.perform(
            patch("/api/v1/flights/flight-001")
                .with(operator())
                .header("If-Match", "\"1\"")
                .contentType("application/json")
                .content("{\"status\":\"BOARDING\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"2\""));
    mvc.perform(
            patch("/api/v1/flights/flight-001")
                .with(operator())
                .header("If-Match", "\"1\"")
                .contentType("application/json")
                .content("{\"status\":\"BOARDING\"}"))
        .andExpect(status().isPreconditionFailed());
  }

  @Test
  void missingPrecondition() throws Exception {
    mvc.perform(
            patch("/api/v1/flights/flight-001")
                .with(operator())
                .contentType("application/json")
                .content("{\"status\":\"BOARDING\"}"))
        .andExpect(status().is(428));
  }

  @Test
  void rejectsMassAssignment() throws Exception {
    mvc.perform(
            patch("/api/v1/flights/flight-001")
                .with(operator())
                .header("If-Match", "\"1\"")
                .contentType("application/json")
                .content("{\"terminalId\":\"T2\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsNullStatus() throws Exception {
    mvc.perform(
            patch("/api/v1/flights/flight-001")
                .with(operator())
                .header("If-Match", "\"1\"")
                .contentType("application/json")
                .content("{\"status\":null}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void revokedSubjectRejected() throws Exception {
    when(redis.hasKey("aobp:user:blocked:ops")).thenReturn(true);
    mvc.perform(get("/api/v1/flights").with(viewer())).andExpect(status().isUnauthorized());
  }

  @Test
  void unavailableRevocationFailsClosed() throws Exception {
    when(redis.hasKey(anyString()))
        .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("down"));
    mvc.perform(get("/api/v1/flights").with(viewer())).andExpect(status().isServiceUnavailable());
  }
}
