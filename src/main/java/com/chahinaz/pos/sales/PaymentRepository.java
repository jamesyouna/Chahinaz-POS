package com.chahinaz.pos.sales;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentRepository extends JpaRepository<Payment,UUID>{}
