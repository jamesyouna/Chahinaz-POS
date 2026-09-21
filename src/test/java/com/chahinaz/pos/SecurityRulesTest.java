package com.chahinaz.pos;

import com.chahinaz.pos.admin.AdminController;
import com.chahinaz.pos.audit.AuditService;
import com.chahinaz.pos.employee.EmployeeRepository;
import com.chahinaz.pos.employee.Employee;
import com.chahinaz.pos.employee.Role;
import com.chahinaz.pos.security.CurrentUserController;
import com.chahinaz.pos.security.SecurityConfig;
import com.chahinaz.pos.settings.SettingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import java.util.Optional;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WebMvcTest({CurrentUserController.class, AdminController.class})
@Import(SecurityConfig.class)
class SecurityRulesTest {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwordEncoder;
  @MockitoBean EmployeeRepository employees;
  @MockitoBean SettingRepository settings;
  @MockitoBean AuditService audit;

  @Test void anonymousCannotReadAdminData() throws Exception {
    mvc.perform(get("/api/admin/employees")).andExpect(status().is3xxRedirection());
  }

  @Test void tellerCannotReadAdminData() throws Exception {
    mvc.perform(get("/api/admin/employees").with(user("teller").roles("TELLER"))).andExpect(status().isForbidden());
  }

  @Test void adminCanReadEmployeeList() throws Exception {
    when(employees.findAll()).thenReturn(java.util.List.of());
    mvc.perform(get("/api/admin/employees").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  }

  @Test void tellerCanSeeOwnSession() throws Exception {
    mvc.perform(get("/api/auth/me").with(user("teller").roles("TELLER"))).andExpect(status().isOk());
  }

  @Test void formLoginCreatesSessionThatAuthenticatesCurrentUser() throws Exception {
    var admin = new Employee("admin", "Admin", passwordEncoder.encode("correct-password"), Role.ADMIN);
    when(employees.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
    MvcResult result = mvc.perform(post("/login").with(csrf())
        .param("username", "admin").param("password", "correct-password"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"))
        .andExpect(authenticated().withUsername("admin")).andReturn();
    mvc.perform(get("/api/auth/me").session((MockHttpSession) result.getRequest().getSession(false)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("admin"));
  }

  @Test void invalidFormLoginDoesNotCreateAuthenticatedSession() throws Exception {
    var admin = new Employee("admin", "Admin", passwordEncoder.encode("correct-password"), Role.ADMIN);
    when(employees.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
    mvc.perform(post("/login").with(csrf())
        .param("username", "admin").param("password", "wrong-password"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error"))
        .andExpect(unauthenticated());
  }

  @Test void loginStillRequiresCsrf() throws Exception {
    mvc.perform(post("/login").param("username", "admin").param("password", "anything"))
        .andExpect(status().isForbidden()).andExpect(unauthenticated());
  }

  @Test void spaCsrfHandlerAcceptsRawCookieTokenInHeader() {
    var request = new MockHttpServletRequest();
    request.addHeader("X-XSRF-TOKEN", "raw-cookie-token");
    var token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "raw-cookie-token");
    assertEquals("raw-cookie-token",
        new SecurityConfig.SpaCsrfTokenRequestHandler().resolveCsrfTokenValue(request, token));
  }
}
