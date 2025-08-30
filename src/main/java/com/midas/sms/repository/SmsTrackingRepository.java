package com.midas.sms.repository;

import com.midas.sms.entity.SmsTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmsTrackingRepository extends JpaRepository<SmsTracking, Long> {
    
    Optional<SmsTracking> findByCodigoTracking(String codigoTracking);
    
    List<SmsTracking> findByUserId(Long userId);
    
    List<SmsTracking> findByUserIdAndVisitado(Long userId, Boolean visitado);
    
    @Query("SELECT st FROM SmsTracking st WHERE st.userId = :userId AND st.fechaEnvio BETWEEN :fechaInicio AND :fechaFin")
    List<SmsTracking> findByUserIdAndFechaEnvioBetween(
            @Param("userId") Long userId, 
            @Param("fechaInicio") LocalDateTime fechaInicio, 
            @Param("fechaFin") LocalDateTime fechaFin);
    
    @Query("SELECT COUNT(st) FROM SmsTracking st WHERE st.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(st) FROM SmsTracking st WHERE st.userId = :userId AND st.visitado = true")
    Long countVisitadosByUserId(@Param("userId") Long userId);
    
    @Query("SELECT st FROM SmsTracking st WHERE st.visitado = false AND st.fechaEnvio < :fechaLimite")
    List<SmsTracking> findNoVisitadosAnterioresA(@Param("fechaLimite") LocalDateTime fechaLimite);
    
    boolean existsByCodigoTracking(String codigoTracking);
}