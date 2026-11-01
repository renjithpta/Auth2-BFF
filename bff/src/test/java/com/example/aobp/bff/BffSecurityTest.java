package com.example.aobp.bff;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "OIDC_CLIENT_SECRET=test-only",
      "TOKEN_ENCRYPTION_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
      "management.server.port=0",
      "spring.session.store-type=none",
      "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.session.SessionAutoConfiguration"
    })
@AutoConfigureMockMvc
class BffSecurityTest {
  @Autowired MockMvc mvc;
  @MockitoBean StringRedisTemplate redis;
  @MockitoBean SessionRepository<?> sessions;

  @Test
  void api401NeverLoginHtmlRedirect() throws Exception {
    mvc.perform(get("/bff/api/v1/session"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }

  @Test
  void loginUsesPkce() throws Exception {
    mvc.perform(get("/oauth2/authorization/aobp"))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            header()
                .string(
                    "Location",
                    org.hamcrest.Matchers.containsString("code_challenge_method=S256")));
  }

  @Test
  void logoutWithoutCsrfDenied() throws Exception {
    mvc.perform(post("/logout")).andExpect(status().isForbidden());
  }

  @Test
  void csrfBootstrapAvailable() throws Exception {
    mvc.perform(get("/bff/api/v1/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void ordinaryUserCannotBlockAccount() throws Exception {
    mvc.perform(post("/bff/api/v1/admin/blocked-users/subject").with(oidcLogin()).with(csrf()))
        .andExpect(status().isForbidden());
  }
}
