package com.chahinaz.pos.operations;
import java.util.*;import org.springframework.data.jpa.repository.*;import jakarta.persistence.LockModeType;
public interface RegisterSessionRepository extends JpaRepository<RegisterSession,UUID>{
 Optional<RegisterSession> findByEmployeeIdAndStatus(UUID employeeId,RegisterStatus status);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from RegisterSession r where r.id=:id") Optional<RegisterSession> findLocked(UUID id);
 List<RegisterSession> findByEmployeeIdOrderByOpenedAtDesc(UUID id);
}
