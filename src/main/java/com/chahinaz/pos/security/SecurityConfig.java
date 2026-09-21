package com.chahinaz.pos.security;

import com.chahinaz.pos.employee.EmployeeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

  @Bean UserDetailsService userDetailsService(EmployeeRepository employees) {
    return username -> {
      var employee = employees.findByUsernameIgnoreCase(username)
          .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
      return User.withUsername(employee.username).password(employee.passwordHash)
          .authorities("ROLE_" + employee.role.name()).disabled(!employee.enabled).build();
    };
  }

  @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.GET,"/","/index.html","/assets/**","/favicon.ico","/health","/sell","/sales","/registers","/reports","/inventory","/products","/admin").permitAll()
        .requestMatchers(HttpMethod.GET,"/api/public/catalogue/**","/api/public/images/**").permitAll()
        .requestMatchers(HttpMethod.HEAD,"/api/public/catalogue/**","/api/public/images/**").permitAll()
        .requestMatchers(HttpMethod.OPTIONS,"/api/public/catalogue/**","/api/public/images/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/public/**").denyAll()
        .requestMatchers("/api/pos/sales/**").hasAnyRole("ADMIN","MANAGER","TELLER")
        .requestMatchers("/api/pos/sales").hasAnyRole("ADMIN","MANAGER","TELLER")
        .requestMatchers("/api/pos/registers/**").hasAnyRole("ADMIN","MANAGER","TELLER")
        .requestMatchers("/api/pos/approvals").hasAnyRole("ADMIN","MANAGER","TELLER")
        .requestMatchers(HttpMethod.GET,"/api/pos/**").hasAnyRole("ADMIN","MANAGER","TELLER")
        .requestMatchers("/api/pos/**").denyAll()
        .requestMatchers("/api/management/**").hasAnyRole("ADMIN", "MANAGER")
        .requestMatchers("/api/**").authenticated()
        .anyRequest().denyAll())
      .cors(Customizer.withDefaults())
      .formLogin(form -> form.permitAll())
      .logout(logout -> logout.logoutUrl("/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
      .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
    return http.build();
  }
}
