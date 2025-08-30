package com.midas.sms.controller;

import com.midas.sms.dto.ApiResponse;
import com.midas.sms.dto.SmsMessageDTO;
import com.midas.sms.entity.SmsMessage;
import com.midas.sms.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/sms/test")
@CrossOrigin(origins = "*")
public class SmsTestController {
    
    @Autowired
    private SmsService smsService;
    
    /**
     * Endpoint de prueba para enviar un mensaje de ejemplo
     */
    @PostMapping("/send-example")
    public ResponseEntity<ApiResponse<SmsMessage>> sendExampleMessage() {
        try {
            SmsMessage sentMessage = smsService.sendSingleMessage(
                "GOOD PIZZA",
                "34666666111",
                "¡Hola! Este es un mensaje de prueba del sistema SMS. ¡Funciona perfectamente!",
                LocalDateTime.now()
            );
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Mensaje de prueba enviado exitosamente", sentMessage));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "Error al enviar mensaje de prueba: " + e.getMessage(), null));
        }
    }
    
    /**
     * Endpoint de prueba para enviar múltiples mensajes de ejemplo
     */
    @PostMapping("/send-batch-example")
    public ResponseEntity<ApiResponse<List<SmsMessage>>> sendBatchExampleMessages() {
        try {
            List<SmsMessageDTO> messages = new ArrayList<>();
            
            // Mensaje 1
            messages.add(new SmsMessageDTO(
                "GOOD PIZZA",
                "34666666111",
                "¡Hola Juan! Hoy tenemos 2x1 en pizzas. ¡Disfruta el partido como un jefe con nuestra nueva pizza de pepperoni!",
                LocalDateTime.now()
            ));
            
            // Mensaje 2
            messages.add(new SmsMessageDTO(
                "GOOD PIZZA",
                "34666666112",
                "¡Hola María! Hoy tenemos 2x1 en pizzas. ¡Disfruta el partido como una jefa con nuestra nueva pizza de pepperoni!",
                LocalDateTime.now()
            ));
            
            // Mensaje programado para el futuro
            messages.add(new SmsMessageDTO(
                "GOOD PIZZA",
                "34666666113",
                "¡Recordatorio! Tu pizza estará lista en 10 minutos.",
                LocalDateTime.now().plusMinutes(5)
            ));
            
            List<SmsMessage> sentMessages = smsService.sendBatchMessages(messages);
            
            return ResponseEntity.ok(new ApiResponse<>(true, 
                    "Lote de mensajes de prueba enviado exitosamente", sentMessages));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "Error al enviar lote de mensajes de prueba: " + e.getMessage(), null));
        }
    }
    
    /**
     * Endpoint de prueba para enviar un mensaje con supervisión de usuario
     */
    @PostMapping("/send-example-with-user")
    public ResponseEntity<ApiResponse<SmsMessage>> sendExampleMessageWithUser(@RequestParam Long userId) {
        try {
            SmsMessage sentMessage = smsService.sendSingleMessage(
                "GOOD PIZZA",
                "34666666111",
                "¡Hola! Este es un mensaje de prueba con supervisión. Usuario ID: " + userId,
                LocalDateTime.now(),
                userId
            );
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Mensaje de prueba con supervisión enviado exitosamente", sentMessage));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "Error al enviar mensaje de prueba: " + e.getMessage(), null));
        }
    }
    
    /**
     * Endpoint de prueba para enviar múltiples mensajes con supervisión
     */
    @PostMapping("/send-batch-example-with-user")
    public ResponseEntity<ApiResponse<List<SmsMessage>>> sendBatchExampleMessagesWithUser(@RequestParam Long userId) {
        try {
            List<SmsMessageDTO> messages = new ArrayList<>();
            
            // Mensaje 1
            messages.add(new SmsMessageDTO(
                "GOOD PIZZA",
                "34666666111",
                "¡Hola Juan! Mensaje supervisado por usuario " + userId + ". Promoción 2x1 en pizzas!",
                LocalDateTime.now()
            ));
            
            // Mensaje 2
            messages.add(new SmsMessageDTO(
                "GOOD PIZZA",
                "34666666112",
                "¡Hola María! Mensaje supervisado por usuario " + userId + ". Promoción 2x1 en pizzas!",
                LocalDateTime.now()
            ));
            
            // Mensaje programado
            messages.add(new SmsMessageDTO(
                "GOOD PIZZA",
                "34666666113",
                "¡Recordatorio supervisado! Usuario " + userId + ". Tu pizza estará lista en 10 minutos.",
                LocalDateTime.now().plusMinutes(5)
            ));
            
            List<SmsMessage> sentMessages = smsService.sendBatchMessages(messages, userId);
            
            return ResponseEntity.ok(new ApiResponse<>(true, 
                    "Lote de mensajes de prueba con supervisión enviado exitosamente", sentMessages));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "Error al enviar lote de mensajes de prueba: " + e.getMessage(), null));
        }
    }
    
    /**
     * Endpoint para obtener estadísticas de mensajes
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getMessageStats() {
        try {
            List<SmsMessage> allMessages = smsService.getAllMessages();
            
            long pending = allMessages.stream().filter(m -> m.getStatus() == SmsMessage.MessageStatus.PENDING).count();
            long sent = allMessages.stream().filter(m -> m.getStatus() == SmsMessage.MessageStatus.SENT).count();
            long delivered = allMessages.stream().filter(m -> m.getStatus() == SmsMessage.MessageStatus.DELIVERED).count();
            long failed = allMessages.stream().filter(m -> m.getStatus() == SmsMessage.MessageStatus.FAILED).count();
            long error = allMessages.stream().filter(m -> m.getStatus() == SmsMessage.MessageStatus.ERROR).count();
            
            Object stats = new Object() {
                public final long total = allMessages.size();
                public final long pendingCount = pending;
                public final long sentCount = sent;
                public final long deliveredCount = delivered;
                public final long failedCount = failed;
                public final long errorCount = error;
            };
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Estadísticas obtenidas exitosamente", stats));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "Error al obtener estadísticas: " + e.getMessage(), null));
        }
    }
}