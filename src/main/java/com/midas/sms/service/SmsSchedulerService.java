package com.midas.sms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SmsSchedulerService {
    
    private static final Logger logger = LoggerFactory.getLogger(SmsSchedulerService.class);
    
    @Autowired
    private SmsService smsService;
    
    /**
     * Procesa mensajes pendientes cada minuto
     */
    @Scheduled(fixedRate = 60000) // Cada 60 segundos
    public void processPendingMessages() {
        try {
            logger.debug("Verificando mensajes pendientes de envío...");
            smsService.processPendingMessages();
        } catch (Exception e) {
            logger.error("Error al procesar mensajes pendientes: ", e);
        }
    }
}