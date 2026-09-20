package com.chahinaz.pos.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.math.BigDecimal;
import com.chahinaz.pos.audit.AuditService;
import com.chahinaz.pos.employee.*;
import com.chahinaz.pos.settings.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private static final Set<String> ALLOWED_SETTINGS = Set.of("store.name", "store.slogan", "store.phone", "store.address", "currency.usd_to_lbp", "inventory.low_stock_default");
  private final EmployeeRepository employees;
  private final SettingRepository settings;
  private final PasswordEncoder encoder;
  private final AuditService audit;
  public AdminController(EmployeeRepository employees, SettingRepository settings, PasswordEncoder encoder, AuditService audit) {
    this.employees = employees; this.settings = settings; this.encoder = encoder; this.audit = audit;
  }
  public record EmployeeInput(@NotBlank @Size(max=80) String username, @NotBlank @Size(max=120) String displayName,
      @NotBlank @Size(min=16) String password, @NotNull Role role) {}
  public record EmployeeView(UUID id, String username, String displayName, Role role, boolean enabled) {
    static EmployeeView of(Employee e) { return new EmployeeView(e.id,e.username,e.displayName,e.role,e.enabled); }
  }
  public record SettingInput(@NotBlank @Size(max=1000) String value) {}
  public record SettingView(String key, String value, Instant updatedAt) {
    static SettingView of(StoreSetting s) { return new SettingView(s.key,s.value,s.updatedAt); }
  }
  private UUID actor(Authentication auth) { return employees.findByUsernameIgnoreCase(auth.getName()).orElseThrow().id; }

  @GetMapping("/employees") public List<EmployeeView> employees() { return employees.findAll().stream().map(EmployeeView::of).toList(); }

  @PostMapping("/employees") @ResponseStatus(HttpStatus.CREATED) @Transactional
  public EmployeeView create(@Valid @RequestBody EmployeeInput input, Authentication auth) {
    String username = input.username().trim();
    if (employees.findByUsernameIgnoreCase(username).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
    Employee e = employees.save(new Employee(username,input.displayName().trim(),encoder.encode(input.password()),input.role()));
    audit.record(actor(auth),"EMPLOYEE_CREATED","employee",e.id.toString(),null,e.role.name());
    return EmployeeView.of(e);
  }

  @PatchMapping("/employees/{id}/enabled") @Transactional
  public EmployeeView enabled(@PathVariable UUID id, @RequestBody boolean enabled, Authentication auth) {
    Employee e = employees.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (e.id.equals(actor(auth)) && !enabled) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot disable your own account");
    boolean before=e.enabled; e.enabled=enabled; e.updatedAt=Instant.now(); employees.save(e);
    audit.record(actor(auth), enabled ? "EMPLOYEE_ENABLED" : "EMPLOYEE_DISABLED","employee",id.toString(),String.valueOf(before),String.valueOf(enabled));
    return EmployeeView.of(e);
  }

  @GetMapping("/settings") public List<SettingView> settings() { return settings.findAll().stream().map(SettingView::of).toList(); }

  @PutMapping("/settings/{key}") @Transactional
  public SettingView setting(@PathVariable String key, @Valid @RequestBody SettingInput input, Authentication auth) {
    if (!ALLOWED_SETTINGS.contains(key)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported setting key");
    if (key.equals("currency.usd_to_lbp")) {
      try { if (new BigDecimal(input.value()).signum() <= 0) throw new NumberFormatException(); }
      catch (NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Exchange rate must be positive"); }
    }
    if (key.equals("inventory.low_stock_default")) {
      try { if (Integer.parseInt(input.value()) < 0) throw new NumberFormatException(); }
      catch (NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Low-stock threshold must be nonnegative"); }
    }
    StoreSetting s = settings.findById(key).orElseGet(() -> new StoreSetting(key,input.value(),actor(auth)));
    String before = s.value; s.value=input.value(); s.updatedBy=actor(auth); s.updatedAt=Instant.now(); settings.save(s);
    audit.record(actor(auth),"SETTING_CHANGED","store_setting",key,before,s.value);
    return SettingView.of(s);
  }
}
