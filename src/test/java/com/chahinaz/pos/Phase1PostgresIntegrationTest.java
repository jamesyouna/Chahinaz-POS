package com.chahinaz.pos;

import com.chahinaz.pos.employee.Employee;
import com.chahinaz.pos.employee.EmployeeRepository;
import com.chahinaz.pos.employee.Role;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties = {
    "POS_BOOTSTRAP_ADMIN_USERNAME=bootstrap",
    "POS_BOOTSTRAP_ADMIN_PASSWORD=development-test-password-only",
    "POS_SECURE_COOKIE=false"
})
@AutoConfigureMockMvc
class Phase1PostgresIntegrationTest {
  @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", postgres::getJdbcUrl);
    properties.add("spring.datasource.username", postgres::getUsername);
    properties.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired EmployeeRepository employees;
  @Autowired PasswordEncoder encoder;
  @Autowired JdbcTemplate jdbc;

  @Test void migrationAndInitialAdminBootstrap() {
    Employee admin = employees.findByUsernameIgnoreCase("bootstrap").orElseThrow();
    assertEquals(Role.ADMIN, admin.role);
    assertNotEquals("development-test-password-only", admin.passwordHash);
    assertTrue(encoder.matches("development-test-password-only", admin.passwordHash));
    assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='1' AND success=true", Integer.class));
    assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_name='audit_event'", Integer.class));
  }

  @Test void realLoginSessionCsrfAndLogout() throws Exception {
    var result = mvc.perform(post("/login").with(csrf()).param("username", "bootstrap")
        .param("password", "development-test-password-only"))
        .andExpect(status().is3xxRedirection()).andReturn();
    HttpSession session = result.getRequest().getSession(false);
    assertNotNull(session);
    mvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession) session))
        .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("bootstrap"));
    mvc.perform(put("/api/admin/settings/store.name").session((org.springframework.mock.web.MockHttpSession) session)
        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"Chahina'z\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/logout").session((org.springframework.mock.web.MockHttpSession) session).with(csrf()))
        .andExpect(status().is3xxRedirection());
    mvc.perform(get("/api/auth/me"))
        .andExpect(status().is3xxRedirection());
  }

  @Test void roleBoundariesEmployeeLoginAndAudit() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    for (Role role : new Role[] {Role.MANAGER, Role.TELLER}) {
      String name = role.name().toLowerCase() + suffix;
      mvc.perform(post("/api/admin/employees").with(user("bootstrap").roles("ADMIN")).with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"username\":\"" + name + "\",\"displayName\":\"Test " + role + "\",\"password\":\"test-employee-password-123\",\"role\":\"" + role + "\"}"))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value(role.name()));
      mvc.perform(post("/login").with(csrf()).param("username", name)
          .param("password", "test-employee-password-123"))
          .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
    mvc.perform(get("/api/admin/employees").with(user(name).roles(role.name())))
          .andExpect(status().isForbidden());
      mvc.perform(get("/api/auth/me").with(user(name).roles(role.name())))
          .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(name));
    }
    mvc.perform(get("/api/management/not-yet-implemented").with(user("manager").roles("MANAGER")))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/management/not-yet-implemented").with(user("teller").roles("TELLER")))
        .andExpect(status().isForbidden());
    Employee teller = employees.findByUsernameIgnoreCase("teller" + suffix).orElseThrow();
    mvc.perform(patch("/api/admin/employees/{id}/enabled", teller.id)
        .with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("false"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
    mvc.perform(post("/login").with(csrf()).param("username", teller.username)
        .param("password", "test-employee-password-123"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error"));
    assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='EMPLOYEE_CREATED'", Integer.class));
    assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='EMPLOYEE_DISABLED'", Integer.class));
    assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE after_value::text LIKE '%password%'", Integer.class));
  }

  @Test void settingsValidationAndAuditAreAtomic() throws Exception {
    mvc.perform(put("/api/admin/settings/currency.usd_to_lbp").with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"89500\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.value").value("89500"));
    mvc.perform(put("/api/admin/settings/currency.usd_to_lbp").with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"-1\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(put("/api/admin/settings/private.token").with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"secret\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/admin/settings").with(user("bootstrap").roles("ADMIN")))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].value").value("89500"));
    mvc.perform(get("/api/admin/settings").with(user("manager").roles("MANAGER")))
        .andExpect(status().isForbidden());
    assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='SETTING_CHANGED' AND entity_id='currency.usd_to_lbp'", Integer.class));
  }
}
