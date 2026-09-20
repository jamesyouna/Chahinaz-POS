package com.chahinaz.pos.security;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController {
  @GetMapping("/api/auth/me")
  public Map<String, Object> me(Authentication authentication) {
    return Map.of("username", authentication.getName(), "authorities", authentication.getAuthorities());
  }
}
