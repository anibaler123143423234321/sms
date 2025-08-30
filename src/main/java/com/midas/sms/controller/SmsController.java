package com.midas.sms.controller;

import com.midas.sms.dto.ApiResponse;
import com.midas.sms.dto.SmsMessageDTO;
import com.midas.sms.dto.SmsMessageWithUserDTO;
import com.midas.sms.dto.SmsEnvioFrontendDTO;
import com.midas.sms.dto.SmsTrackingResponseDTO;
import com.midas.sms.entity.SmsMessage;
import com.midas.sms.entity.SmsTracking;
import com.midas.sms.service.SmsService;
import com.midas.sms.service.SmsTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sms")
public class SmsController {

    @Autowired
    private SmsService smsService;

    @Autowired
    private SmsTrackingService smsTrackingService;

    /**
     * Envía un mensaje SMS individual
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SmsMessage>> sendSingleMessage(@RequestBody SmsMessageDTO messageDTO) {
        try {
            SmsMessage sentMessage = smsService.sendSingleMessage(
                    messageDTO.getFrom(),
                    messageDTO.getTo(),
                    messageDTO.getText(),
                    messageDTO.getSendAt());

            return ResponseEntity.ok(new ApiResponse<>(true, "Mensaje enviado exitosamente", sentMessage));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al enviar mensaje: " + e.getMessage(), null));
        }
    }

    /**
     * Envía múltiples mensajes SMS
     */
    @PostMapping("/send-batch")
    public ResponseEntity<ApiResponse<List<SmsMessage>>> sendBatchMessages(@RequestBody List<SmsMessageDTO> messages) {
        try {
            List<SmsMessage> sentMessages = smsService.sendBatchMessages(messages);

            return ResponseEntity.ok(new ApiResponse<>(true,
                    "Lote de " + sentMessages.size() + " mensajes procesado exitosamente", sentMessages));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al enviar lote de mensajes: " + e.getMessage(), null));
        }
    }

    /**
     * Obtiene todos los mensajes
     */
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<SmsMessage>>> getAllMessages() {
        try {
            List<SmsMessage> messages = smsService.getAllMessages();
            return ResponseEntity.ok(new ApiResponse<>(true, "Mensajes obtenidos exitosamente", messages));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al obtener mensajes: " + e.getMessage(), null));
        }
    }

    /**
     * Obtiene mensajes por estado
     */
    @GetMapping("/messages/status/{status}")
    public ResponseEntity<ApiResponse<List<SmsMessage>>> getMessagesByStatus(@PathVariable String status) {
        try {
            SmsMessage.MessageStatus messageStatus = SmsMessage.MessageStatus.valueOf(status.toUpperCase());
            List<SmsMessage> messages = smsService.getMessagesByStatus(messageStatus);

            return ResponseEntity.ok(new ApiResponse<>(true,
                    "Mensajes con estado " + status + " obtenidos exitosamente", messages));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Estado inválido: " + status, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al obtener mensajes: " + e.getMessage(), null));
        }
    }

    /**
     * Obtiene un mensaje por ID
     */
    @GetMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<SmsMessage>> getMessageById(@PathVariable Long id) {
        try {
            SmsMessage message = smsService.getMessageById(id);

            if (message != null) {
                return ResponseEntity.ok(new ApiResponse<>(true, "Mensaje encontrado", message));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al obtener mensaje: " + e.getMessage(), null));
        }
    }

    /**
     * Procesa mensajes pendientes manualmente
     */
    @PostMapping("/process-pending")
    public ResponseEntity<ApiResponse<String>> processPendingMessages() {
        try {
            smsService.processPendingMessages();
            return ResponseEntity.ok(new ApiResponse<>(true, "Mensajes pendientes procesados", "OK"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al procesar mensajes pendientes: " + e.getMessage(), null));
        }
    }

    // ========== NUEVOS ENDPOINTS CON SUPERVISIÓN DE USUARIOS ==========

    /**
     * Envía un mensaje SMS individual con supervisión de usuario
     */
    @PostMapping("/send-with-user")
    public ResponseEntity<ApiResponse<SmsMessage>> sendSingleMessageWithUser(
            @RequestBody SmsMessageWithUserDTO messageDTO) {
        try {
            SmsMessage sentMessage = smsService.sendSingleMessage(
                    messageDTO.getFrom(),
                    messageDTO.getTo(),
                    messageDTO.getText(),
                    messageDTO.getSendAt(),
                    messageDTO.getUserId());

            return ResponseEntity
                    .ok(new ApiResponse<>(true, "Mensaje enviado exitosamente con supervisión", sentMessage));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al enviar mensaje: " + e.getMessage(), null));
        }
    }

    /**
     * Envía múltiples mensajes SMS con supervisión de usuario
     */
    @PostMapping("/send-batch-with-user")
    public ResponseEntity<ApiResponse<List<SmsMessage>>> sendBatchMessagesWithUser(
            @RequestBody List<SmsMessageDTO> messages,
            @RequestParam Long userId) {
        try {
            List<SmsMessage> sentMessages = smsService.sendBatchMessages(messages, userId);

            return ResponseEntity.ok(new ApiResponse<>(true,
                    "Lote de " + sentMessages.size() + " mensajes procesado exitosamente con supervisión",
                    sentMessages));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al enviar lote de mensajes: " + e.getMessage(), null));
        }
    }

    /**
     * Obtiene mensajes por usuario
     */
    @GetMapping("/messages/user/{userId}")
    public ResponseEntity<ApiResponse<List<SmsMessage>>> getMessagesByUser(@PathVariable Long userId) {
        try {
            List<SmsMessage> messages = smsService.getMessagesByUser(userId);
            return ResponseEntity.ok(new ApiResponse<>(true,
                    "Mensajes del usuario " + userId + " obtenidos exitosamente", messages));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al obtener mensajes del usuario: " + e.getMessage(), null));
        }
    }

    /**
     * Obtiene mensajes supervisados por un coordinador
     */
    @GetMapping("/messages/supervisor/{supervisorId}")
    public ResponseEntity<ApiResponse<List<SmsMessage>>> getMessagesBySupervisor(@PathVariable Long supervisorId) {
        try {
            List<SmsMessage> messages = smsService.getMessagesBySupervisor(supervisorId);
            return ResponseEntity.ok(new ApiResponse<>(true,
                    "Mensajes supervisados por " + supervisorId + " obtenidos exitosamente", messages));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al obtener mensajes supervisados: " + e.getMessage(), null));
        }
    }

    // ========== ENDPOINTS ESPECÍFICOS PARA FRONTEND CON TRACKING ==========

    /**
     * ENDPOINT PRINCIPAL - Envía SMS desde el frontend con tracking automático
     */
    @PostMapping("/enviar-desde-frontend")
    public ResponseEntity<ApiResponse<SmsMessage>> enviarSmsDesdeFrontend(
            @RequestBody SmsEnvioFrontendDTO smsDTO) {
        try {
            SmsMessage sentMessage = smsService.enviarSmsDesdeFrontend(smsDTO);

            return ResponseEntity.ok(new ApiResponse<>(true,
                    "SMS enviado exitosamente con código de tracking: " + sentMessage.getCodigoTracking(),
                    sentMessage));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al enviar SMS: " + e.getMessage(), null));
        }
    }

    /**
     * ENDPOINT PARA LANDING - Registra visita desde la landing page
     */
    @GetMapping("/tracking/{codigo}")
    public ResponseEntity<ApiResponse<String>> registrarVisitaTracking(
            @PathVariable String codigo,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {
        try {
            // Obtener IP real del visitante
            String ipVisita = obtenerIpReal(request, xForwardedFor);

            boolean visitaRegistrada = smsTrackingService.registrarVisita(codigo, ipVisita, userAgent);

            if (visitaRegistrada) {
                return ResponseEntity.ok(new ApiResponse<>(true,
                        "Visita registrada exitosamente", "OK"));
            } else {
                return ResponseEntity.ok(new ApiResponse<>(false,
                        "Código no encontrado o ya visitado", "NOT_FOUND"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al registrar visita: " + e.getMessage(), null));
        }
    }

    /**
     * Obtiene estadísticas de tracking por usuario
     */
    @GetMapping("/tracking/estadisticas/{userId}")
    public ResponseEntity<ApiResponse<List<SmsTrackingResponseDTO>>> obtenerEstadisticasTracking(
            @PathVariable Long userId) {
        try {
            List<SmsTrackingResponseDTO> estadisticas = smsTrackingService.obtenerHistorialPorUsuario(userId);

            return ResponseEntity.ok(new ApiResponse<>(true,
                    "Estadísticas obtenidas exitosamente", estadisticas));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al obtener estadísticas: " + e.getMessage(), null));
        }
    }

    /**
     * Obtiene información de un tracking específico
     */
    @GetMapping("/tracking/info/{codigo}")
    public ResponseEntity<ApiResponse<SmsTrackingResponseDTO>> obtenerInfoTracking(
            @PathVariable String codigo) {
        try {
            var tracking = smsTrackingService.obtenerPorCodigo(codigo);

            if (tracking.isPresent()) {
                // Convertir a DTO (puedes crear un método en el service para esto)
                return ResponseEntity.ok(new ApiResponse<>(true,
                        "Información de tracking obtenida", null)); // Aquí iría el DTO convertido
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error al obtener información: " + e.getMessage(), null));
        }
    }

    /**
     * Método auxiliar para obtener la IP real del visitante
     */
    private String obtenerIpReal(HttpServletRequest request, String xForwardedFor) {
        String ip = xForwardedFor;

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Si hay múltiples IPs, tomar la primera
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}