package com.chahinaz.pos.employee;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
  Optional<Employee> findByUsernameIgnoreCase(String username);
}
