package com.chahinaz.pos.sales;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SaleItemRepository extends JpaRepository<SaleItem,UUID>{}
