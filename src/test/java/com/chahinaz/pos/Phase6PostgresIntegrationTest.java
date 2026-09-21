package com.chahinaz.pos;

import com.chahinaz.pos.employee.*;
import com.chahinaz.pos.operations.ApprovalService;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties={"POS_BOOTSTRAP_ADMIN_USERNAME=bootstrap","POS_BOOTSTRAP_ADMIN_PASSWORD=development-test-password-only"})
@AutoConfigureMockMvc
class Phase6PostgresIntegrationTest {
 @Container static PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:16-alpine");
 @DynamicPropertySource static void db(DynamicPropertyRegistry p){p.add("spring.datasource.url",postgres::getJdbcUrl);p.add("spring.datasource.username",postgres::getUsername);p.add("spring.datasource.password",postgres::getPassword);}
 @Autowired MockMvc mvc;@Autowired EmployeeRepository employees;@Autowired PasswordEncoder encoder;@Autowired ApprovalService approvals;@Autowired JdbcTemplate db;
 Employee teller,manager,admin,inactive;
 @BeforeEach void seed(){db.execute("delete from manager_approval");String suffix=UUID.randomUUID().toString().substring(0,8);teller=employees.save(new Employee("teller-"+suffix,"Teller",encoder.encode("teller-password-123"),Role.TELLER));manager=employees.save(new Employee("manager-"+suffix,"Manager",encoder.encode("manager-password-123"),Role.MANAGER));admin=employees.save(new Employee("admin-"+suffix,"Admin",encoder.encode("admin-password-123"),Role.ADMIN));inactive=new Employee("inactive-"+suffix,"Inactive manager",encoder.encode("inactive-password-123"),Role.MANAGER);inactive.enabled=false;employees.saveAndFlush(inactive);}
 String body(Employee approver,String password,String operation,UUID sale){return "{\"managerUsername\":\""+approver.username+"\",\"managerPassword\":\""+password+"\",\"operationType\":\""+operation+"\",\"saleId\":\""+sale+"\",\"reason\":\"Supervisor approved\"}";}
 String approve(Employee approver,String password,String operation,UUID sale)throws Exception{return mvc.perform(post("/api/pos/approvals").with(user(teller.username).roles("TELLER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(approver,password,operation,sale))).andExpect(status().isCreated()).andExpect(authenticated().withUsername(teller.username).withRoles("TELLER")).andReturn().getResponse().getContentAsString().split("\"id\":\"")[1].split("\"")[0];}
 @Test void managerAndAdminApproveWhileTellerRemainsAuthenticated()throws Exception{UUID sale=UUID.randomUUID();String managerId=approve(manager,"manager-password-123","DISCOUNT",sale);String adminId=approve(admin,"admin-password-123","PRICE_OVERRIDE",sale);assertNotNull(managerId);assertNotNull(adminId);assertEquals(manager.id,db.queryForObject("select approving_employee_id from manager_approval where id=?",UUID.class,UUID.fromString(managerId)));assertTrue(db.queryForObject("select after_value::text like ? from audit_event where entity_id=?",Boolean.class,"%requestedBy="+teller.id+"%",managerId));}
 @Test void badUnknownTellerAndInactiveCredentialsAreRejected()throws Exception{UUID sale=UUID.randomUUID();mvc.perform(post("/api/pos/approvals").with(user(teller.username).roles("TELLER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(manager,"wrong-password","DISCOUNT",sale))).andExpect(status().isUnauthorized());mvc.perform(post("/api/pos/approvals").with(user(teller.username).roles("TELLER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(new Employee("missing","Missing","x",Role.MANAGER),"anything","DISCOUNT",sale))).andExpect(status().isUnauthorized());mvc.perform(post("/api/pos/approvals").with(user(teller.username).roles("TELLER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(teller,"teller-password-123","DISCOUNT",sale))).andExpect(status().isForbidden());mvc.perform(post("/api/pos/approvals").with(user(teller.username).roles("TELLER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(inactive,"inactive-password-123","DISCOUNT",sale))).andExpect(status().isUnauthorized());}
 @Test void approvalIsBoundToRequesterOperationAndSaleAndSingleUse()throws Exception{UUID sale=UUID.randomUUID(),other=UUID.randomUUID(),id=UUID.fromString(approve(manager,"manager-password-123","DISCOUNT",sale));assertThrows(ResponseStatusException.class,()->approvals.consume(id,"PRICE_OVERRIDE",teller.id,sale));assertThrows(ResponseStatusException.class,()->approvals.consume(id,"DISCOUNT",teller.id,other));assertThrows(ResponseStatusException.class,()->approvals.consume(id,"DISCOUNT",admin.id,sale));assertEquals(manager.id,approvals.consume(id,"DISCOUNT",teller.id,sale));assertThrows(ResponseStatusException.class,()->approvals.consume(id,"DISCOUNT",teller.id,sale));}
 @Test void expiredApprovalCannotBeConsumed()throws Exception{UUID sale=UUID.randomUUID(),id=UUID.fromString(approve(admin,"admin-password-123","PRICE_OVERRIDE",sale));db.update("update manager_approval set expires_at=now()-interval '1 second' where id=?",id);assertThrows(ResponseStatusException.class,()->approvals.consume(id,"PRICE_OVERRIDE",teller.id,sale));}
}
