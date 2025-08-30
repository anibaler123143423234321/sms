package com.midas.sms.service;

import com.midas.sms.dto.SmsTrackingResponseDTO;
import com.midas.sms.entity.SmsMessage;
import com.midas.sms.entity.SmsTracking;
import com.midas.sms.repository.SmsTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmsTrackingService {
    
    private final SmsTrackingRepository smsTrackingRepository;
    private static final String BASE_URL = "https://naylampgroup.es";
    
    /**
     * Genera un código único de tracking
     */
    public String generarCodigoTracking(Long userId, String nombreCliente) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("USR%d_%s_%s", userId, timestamp, uuid);
    }
    
    /**
     * Crea un registro de tracking para un SMS
     */
    @Transactional
    public SmsTracking crearTracking(SmsMessage smsMessage, Long userId, String nombreCliente, String numeroTelefono) {
        String codigoTracking = generarCodigoTracking(userId, nombreCliente);
        
        // Verificar que el código sea único (por si acaso)
        while (smsTrackingRepository.existsByCodigoTracking(codigoTracking)) {
            codigoTracking = generarCodigoTracking(userId, nombreCliente);
        }
        
        SmsTracking tracking = new SmsTracking();
        tracking.setCodigoTracking(codigoTracking);
        tracking.setSmsMessage(smsMessage);
        tracking.setUserId(userId);
        tracking.setNombreCliente(nombreCliente);
        tracking.setNumeroTelefono(numeroTelefono);
        tracking.setFechaEnvio(LocalDateTime.now());
        tracking.setVisitado(false);
        
        return smsTrackingRepository.save(tracking);
    }
    
    /**
     * Genera la URL completa con el código de tracking
     */
    public String generarUrlCompleta(String codigoTracking) {
        return String.format("%s?ref=%s", BASE_URL, codigoTracking);
    }
    
    /**
     * Registra una visita desde la landing page
     */
    @Transactional
    public boolean registrarVisita(String codigoTracking, String ipVisita, String userAgent) {
        Optional<SmsTracking> trackingOpt = smsTrackingRepository.findByCodigoTracking(codigoTracking);
        
        if (trackingOpt.isPresent()) {
            SmsTracking tracking = trackingOpt.get();
            
            // Solo registrar si no ha sido visitado antes
            if (!tracking.getVisitado()) {
                tracking.marcarComoVisitado(ipVisita, userAgent);
                smsTrackingRepository.save(tracking);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Obtiene estadísticas de tracking por usuario
     */
    public SmsTrackingResponseDTO obtenerEstadisticasPorUsuario(Long userId) {
        Long totalEnviados = smsTrackingRepository.countByUserId(userId);
        Long totalVisitados = smsTrackingRepository.countVisitadosByUserId(userId);
        
        SmsTrackingResponseDTO stats = new SmsTrackingResponseDTO();
        stats.setUserId(userId);
        // Aquí puedes agregar más campos estadísticos
        
        return stats;
    }
    
    /**
     * Obtiene el historial de tracking por usuario
     */
    public List<SmsTrackingResponseDTO> obtenerHistorialPorUsuario(Long userId) {
        List<SmsTracking> trackings = smsTrackingRepository.findByUserId(userId);
        
        return trackings.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene tracking por código
     */
    public Optional<SmsTracking> obtenerPorCodigo(String codigoTracking) {
        return smsTrackingRepository.findByCodigoTracking(codigoTracking);
    }
    
    /**
     * Convierte entidad a DTO
     */
    private SmsTrackingResponseDTO convertirADTO(SmsTracking tracking) {
        SmsTrackingResponseDTO dto = new SmsTrackingResponseDTO();
        dto.setId(tracking.getId());
        dto.setCodigoTracking(tracking.getCodigoTracking());
        dto.setSmsMessageId(tracking.getSmsMessage() != null ? tracking.getSmsMessage().getId() : null);
        dto.setUserId(tracking.getUserId());
        dto.setNombreCliente(tracking.getNombreCliente());
        dto.setNumeroTelefono(tracking.getNumeroTelefono());
        dto.setFechaEnvio(tracking.getFechaEnvio());
        dto.setFechaVisita(tracking.getFechaVisita());
        dto.setIpVisita(tracking.getIpVisita());
        dto.setVisitado(tracking.getVisitado());
        dto.setUserAgent(tracking.getUserAgent());
        dto.setCreatedAt(tracking.getCreatedAt());
        dto.setUpdatedAt(tracking.getUpdatedAt());
        
        // Agregar información del SMS si existe
        if (tracking.getSmsMessage() != null) {
            SmsMessage sms = tracking.getSmsMessage();
            dto.setEstadoSms(sms.getStatus() != null ? sms.getStatus().toString() : null);
            dto.setMensajeEnviado(sms.getMessageText());
            dto.setCampania(sms.getCampania());
            dto.setTipoSms(sms.getTipoSms());
        }
        
        return dto;
    }
    
    /**
     * Obtiene trackings no visitados anteriores a una fecha
     */
    public List<SmsTracking> obtenerNoVisitadosAnterioresA(LocalDateTime fechaLimite) {
        return smsTrackingRepository.findNoVisitadosAnterioresA(fechaLimite);
    }
}