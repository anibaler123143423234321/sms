package com.midas.sms.repository;

import com.midas.sms.entity.SmsMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SmsMessageRepository extends JpaRepository<SmsMessage, Long> {
    
    // Buscar mensajes por estado
    List<SmsMessage> findByStatus(SmsMessage.MessageStatus status);
    
    // Buscar mensajes por número de destino
    List<SmsMessage> findByToNumber(String toNumber);
    
    // Buscar mensajes por número de origen
    List<SmsMessage> findByFromNumber(String fromNumber);
    
    // Buscar mensajes pendientes de envío
    @Query("SELECT s FROM SmsMessage s WHERE s.status = 'PENDING' AND s.sendAt <= :currentTime")
    List<SmsMessage> findPendingMessagesToSend(@Param("currentTime") LocalDateTime currentTime);
    
    // Buscar mensajes por rango de fechas
    @Query("SELECT s FROM SmsMessage s WHERE s.createdAt BETWEEN :startDate AND :endDate")
    List<SmsMessage> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
    
    // Contar mensajes por estado
    long countByStatus(SmsMessage.MessageStatus status);
}