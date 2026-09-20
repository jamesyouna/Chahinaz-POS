package com.chahinaz.pos;

import com.chahinaz.pos.admin.AdminController;
import com.chahinaz.pos.audit.AuditService;
import com.chahinaz.pos.employee.EmployeeRepository;
import com.chahinaz.pos.security.CurrentUserController;
import com.chahinaz.pos.security.SecurityConfig;
import com.chahinaz.pos.settings.SettingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CurrentUserController.class, AdminController.class})
@Import(SecurityConfig.class)
class SecurityRulesTest {
  @Autowired MockMvc mvc;
  @MockitoBean EmployeeRepository employees;
  @MockitoBean SettingRepository settings;
  @MockitoBean AuditService audit;

  @Test void anonymousCannotReadAdminData() throws Exception {
    mvc.perform(get("/api/admin/employees")).andExpect(status().is3xxRedirection());
  }

  @Test @WithMockUser(roles = "TELLER") void tellerCannotReadAdminData() throws Exception {
    mvc.perform(get("/api/admin/employees")).andExpect(status().isForbidden());
  }

  @Test @WithMockUser(roles = "ADMIN") void adminCanReadEmployeeList() throws Exception {
    when(employees.findAll()).thenReturn(java.util.List.of());
    mvc.perform(get("/api/admin/employees")).andExpect(status().isOk());
  }

  @Test @WithMockUser(roles = "TELLER") void tellerCanSeeOwnSession() throws Exception {
    mvc.perform(get("/api/auth/me")).andExpect(status().isOk());
  }
}
