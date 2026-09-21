package com.chahinaz.pos.operations;
import jakarta.persistence.LockModeType;import java.util.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface ManagerApprovalRepository extends JpaRepository<ManagerApproval,UUID>{@Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select a from ManagerApproval a where a.id=:id")Optional<ManagerApproval> findLocked(@Param("id")UUID id);}
