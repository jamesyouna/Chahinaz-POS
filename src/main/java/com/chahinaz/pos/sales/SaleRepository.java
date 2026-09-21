package com.chahinaz.pos.sales;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface SaleRepository extends JpaRepository<Sale,UUID>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from Sale s where s.id=:id") Optional<Sale> findLocked(UUID id);
 Page<Sale> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId,Pageable pageable);
 Page<Sale> findAllByOrderByCreatedAtDesc(Pageable pageable);
 List<Sale> findByEmployeeIdAndStatusOrderByCreatedAtDesc(UUID employeeId,SaleStatus status);
 List<Sale> findByStatusOrderByCreatedAtDesc(SaleStatus status);
}
