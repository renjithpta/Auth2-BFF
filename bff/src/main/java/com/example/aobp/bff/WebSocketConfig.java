package com.example.aobp.bff;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.session.SessionRepository;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
@EnableScheduling
public class WebSocketConfig extends TextWebSocketHandler implements WebSocketConfigurer {
  private final Map<String, WebSocketSession> sockets = new ConcurrentHashMap<>();
  private final SessionRepository<?> sessions;
  private final StringRedisTemplate redis;
  private final String origin;

  public WebSocketConfig(
      SessionRepository<?> sessions,
      StringRedisTemplate redis,
      @Value("${PUBLIC_ORIGIN:http://localhost:8088}") String origin) {
    this.sessions = sessions;
    this.redis = redis;
    this.origin = origin;
  }

  public void registerWebSocketHandlers(WebSocketHandlerRegistry r) {
    r.addHandler(this, "/bff/ws/v1")
        .addInterceptors(new HttpSessionHandshakeInterceptor())
        .setAllowedOrigins(origin);
  }

  public void afterConnectionEstablished(WebSocketSession s) throws Exception {
    if (s.getPrincipal() == null) {
      s.close(CloseStatus.POLICY_VIOLATION);
      return;
    }
    sockets.put(s.getId(), s);
    s.sendMessage(new TextMessage("{\"type\":\"CONNECTED\",\"protocolVersion\":1}"));
  }

  public void afterConnectionClosed(WebSocketSession s, CloseStatus c) {
    sockets.remove(s.getId());
  }

  protected void handleTextMessage(WebSocketSession s, TextMessage m) throws Exception {
    if (m.getPayloadLength() > 1024 || !m.getPayload().equals("ping")) {
      s.close(CloseStatus.POLICY_VIOLATION);
      return;
    }
    s.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
  }

  @Scheduled(fixedDelay = 5000)
  public void validate() {
    for (var s : sockets.values())
      try {
        Object id =
            s.getAttributes().get(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME);
        if (id == null
            || sessions.findById(id.toString()) == null
            || Boolean.TRUE.equals(
                redis.hasKey("aobp:user:blocked:" + s.getPrincipal().getName()))) {
          s.close(CloseStatus.POLICY_VIOLATION);
          continue;
        }
        synchronized (s) {
          s.sendMessage(new TextMessage("{\"type\":\"REFRESH_HINT\",\"resource\":\"flights\"}"));
        }
      } catch (Exception e) {
        try {
          s.close(CloseStatus.SERVER_ERROR);
        } catch (Exception ignored) {
        }
      }
  }
}
