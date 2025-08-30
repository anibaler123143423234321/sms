package com.midas.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsTrackingResponseDTO {
    
    private Long id;
    private String codigoTracking;
    private Long smsMessageId;
    private Long userId;
    private String nombreCliente;
    private String numeroTelefono;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaVisita;
    private String ipVisita;
    private Boolean visitado;
    private String userAgent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Campos adicionales para estadísticas
    private String estadoSms;
    private String mensajeEnviado;
    private String campania;
    private String tipoSms;
}