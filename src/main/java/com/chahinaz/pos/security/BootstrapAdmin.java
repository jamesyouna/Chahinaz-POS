package com.chahinaz.pos.security;

import com.chahinaz.pos.employee.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdmin {
  @Bean CommandLineRunner createFirstAdmin(EmployeeRepository employees, PasswordEncoder encoder, Environment env) {
    return args -> {
      if (employees.count() != 0) return;
      String username = env.getProperty("POS_BOOTSTRAP_ADMIN_USERNAME");
      String password = env.getProperty("POS_BOOTSTRAP_ADMIN_PASSWORD");
      if (username == null || username.isBlank() || password == null || password.length() < 16)
        throw new IllegalStateException("First start requires POS_BOOTSTRAP_ADMIN_USERNAME and a 16+ character POS_BOOTSTRAP_ADMIN_PASSWORD");
      employees.save(new Employee(username.trim(), "Store Administrator", encoder.encode(password), Role.ADMIN));
    };
  }
}
