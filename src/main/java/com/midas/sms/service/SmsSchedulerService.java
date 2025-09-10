package com.midas.sms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SmsSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SmsSchedulerService.class);

    @Autowired
    private SmsService smsService;

    @Value("${sms.scheduler.enabled:false}")
    private boolean schedulerEnabled;

    /**
     * Procesa mensajes pendientes cada minuto (solo si está habilitado)
     */
    @Scheduled(fixedRate = 60000) // Cada 60 segundos
    public void processPendingMessages() {
        if (!schedulerEnabled) {
            return; // No ejecutar si está deshabilitado
        }

        try {
            logger.debug("Verificando mensajes pendientes de envío...");
            smsService.processPendingMessages();
        } catch (Exception e) {
            logger.error("Error al procesar mensajes pendientes: ", e);
        }
    }
}