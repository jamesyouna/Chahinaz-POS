package com.chahinaz.pos.catalogue;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
  boolean existsByNameIgnoreCase(String name);
  List<Category> findByActiveTrueOrderByNameAsc();
}
