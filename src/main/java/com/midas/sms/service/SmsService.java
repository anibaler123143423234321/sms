package com.midas.sms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.midas.sms.dto.SmsBatchRequestDTO;
import com.midas.sms.dto.SmsMessageDTO;
import com.midas.sms.dto.SmsEnvioFrontendDTO;
import com.midas.sms.entity.SmsMessage;
import com.midas.sms.entity.SmsTracking;
import com.midas.sms.entity.User;
import com.midas.sms.repository.SmsMessageRepository;
import com.midas.sms.repository.UserRepository;
import com.midas.sms.util.IpUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Autowired
    private SmsMessageRepository smsMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SmsTrackingService smsTrackingService;

    @Value("${sms.gateway.api-key:399d2b438a53ebed3db8a7d52107f846}")
    private String apiKey;

    @Value("${sms.gateway.url:https://api.gateway360.com/api/3.0/sms/send}")
    private String gatewayUrl;

    @Value("${sms.gateway.report-url:http://yourserver.com/callback/script}")
    private String reportUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Envía un mensaje SMS individual (método original para compatibilidad)
     */
    public SmsMessage sendSingleMessage(String from, String to, String text, LocalDateTime sendAt) {
        return sendSingleMessage(from, to, text, sendAt, null);
    }

    /**
     * Envía un mensaje SMS individual con supervisión de usuario
     */
    public SmsMessage sendSingleMessage(String from, String to, String text, LocalDateTime sendAt, Long userId) {
        // Crear y configurar el mensaje
        SmsMessage smsMessage = new SmsMessage(from, to, text, sendAt);

        // Configurar usuario y supervisor si se proporciona userId
        if (userId != null) {
            setupUserAndSupervisor(smsMessage, userId);
        }

        // Registrar IP automáticamente
        smsMessage.setIpAddress(IpUtils.getClientIpAddress());

        // Guardar en la base de datos
        smsMessage = smsMessageRepository.save(smsMessage);

        // Si el mensaje debe enviarse ahora, procesarlo
        if (sendAt == null || sendAt.isBefore(LocalDateTime.now()) || sendAt.isEqual(LocalDateTime.now())) {
            return processSingleMessage(smsMessage);
        }

        return smsMessage;
    }

    /**
     * Envía múltiples mensajes SMS (método original para compatibilidad)
     */
    public List<SmsMessage> sendBatchMessages(List<SmsMessageDTO> messageDTOs) {
        return sendBatchMessages(messageDTOs, null);
    }

    /**
     * Envía múltiples mensajes SMS con supervisión de usuario
     */
    public List<SmsMessage> sendBatchMessages(List<SmsMessageDTO> messageDTOs, Long userId) {
        List<SmsMessage> savedMessages = new ArrayList<>();
        List<SmsMessage> messagesToSendNow = new ArrayList<>();

        // Guardar todos los mensajes en la base de datos
        for (SmsMessageDTO dto : messageDTOs) {
            SmsMessage smsMessage = new SmsMessage(dto.getFrom(), dto.getTo(), dto.getText(), dto.getSendAt());

            // Configurar usuario y supervisor si se proporciona userId
            if (userId != null) {
                setupUserAndSupervisor(smsMessage, userId);
            }

            // Registrar IP automáticamente
            smsMessage.setIpAddress(IpUtils.getClientIpAddress());

            smsMessage = smsMessageRepository.save(smsMessage);
            savedMessages.add(smsMessage);

            // Si el mensaje debe enviarse ahora, agregarlo a la lista
            if (dto.getSendAt() == null || dto.getSendAt().isBefore(LocalDateTime.now())
                    || dto.getSendAt().isEqual(LocalDateTime.now())) {
                messagesToSendNow.add(smsMessage);
            }
        }

        // Procesar mensajes que deben enviarse ahora
        if (!messagesToSendNow.isEmpty()) {
            processBatchMessages(messagesToSendNow);
        }

        return savedMessages;
    }

    /**
     * Procesa mensajes pendientes de envío
     */
    public void processPendingMessages() {
        List<SmsMessage> pendingMessages = smsMessageRepository.findPendingMessagesToSend(LocalDateTime.now());

        if (!pendingMessages.isEmpty()) {
            logger.info("Procesando {} mensajes pendientes", pendingMessages.size());
            processBatchMessages(pendingMessages);
        }
    }

    /**
     * Procesa un mensaje individual
     */
    private SmsMessage processSingleMessage(SmsMessage smsMessage) {
        List<SmsMessage> messages = new ArrayList<>();
        messages.add(smsMessage);
        processBatchMessages(messages);
        return smsMessage;
    }

    /**
     * Procesa múltiples mensajes enviándolos al gateway
     */
    private void processBatchMessages(List<SmsMessage> messages) {
        try {
            logger.info("Procesando {} mensajes con API Key: {}", messages.size(), apiKey);
            
            // Preparar la petición para el gateway
            List<SmsMessageDTO> messageDTOs = new ArrayList<>();
            for (SmsMessage msg : messages) {
                messageDTOs.add(new SmsMessageDTO(msg.getFromNumber(), msg.getToNumber(),
                        msg.getMessageText(), msg.getSendAt()));
            }

            SmsBatchRequestDTO batchRequest = new SmsBatchRequestDTO(apiKey, reportUrl, 1, messageDTOs);
            
            logger.info("Enviando petición al gateway: {}", gatewayUrl);
            logger.info("Payload: apiKey={}, reportUrl={}, concat=1, messages={}", 
                       batchRequest.getApiKey(), batchRequest.getReportUrl(), messageDTOs.size());

            // Enviar al gateway
            String response = sendToGateway(batchRequest);

            // Actualizar el estado de los mensajes
            for (SmsMessage msg : messages) {
                msg.setStatus(SmsMessage.MessageStatus.SENT);
                msg.setSentAt(LocalDateTime.now());
                msg.setGatewayResponse(response);
                smsMessageRepository.save(msg);
            }

            logger.info("Enviados {} mensajes exitosamente", messages.size());

        } catch (Exception e) {
            logger.error("Error al enviar mensajes: ", e);

            // Marcar mensajes como fallidos
            for (SmsMessage msg : messages) {
                msg.setStatus(SmsMessage.MessageStatus.FAILED);
                msg.setGatewayResponse("Error: " + e.getMessage());
                smsMessageRepository.save(msg);
            }
        }
    }

    /**
     * Envía la petición al gateway SMS
     */
    private String sendToGateway(SmsBatchRequestDTO batchRequest) throws Exception {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(gatewayUrl);

        // Convertir el objeto a JSON
        String jsonPayload = objectMapper.writeValueAsString(batchRequest);
        
        logger.info("JSON Payload enviado al gateway: {}", jsonPayload);

        StringEntity params = new StringEntity(jsonPayload, "UTF-8");
        request.addHeader("content-type", "application/json");
        request.addHeader("Accept", "application/json");
        request.setEntity(params);

        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        String responseBody = EntityUtils.toString(response.getEntity());

        logger.info("Gateway response status: {}, body: {}", statusCode, responseBody);

        return responseBody;
    }

    /**
     * Obtiene todos los mensajes
     */
    public List<SmsMessage> getAllMessages() {
        return smsMessageRepository.findAll();
    }

    /**
     * Obtiene mensajes por estado
     */
    public List<SmsMessage> getMessagesByStatus(SmsMessage.MessageStatus status) {
        return smsMessageRepository.findByStatus(status);
    }

    /**
     * Obtiene un mensaje por ID
     */
    public SmsMessage getMessageById(Long id) {
        return smsMessageRepository.findById(id).orElse(null);
    }

    /**
     * Configura el usuario y supervisor para un mensaje SMS
     */
    private void setupUserAndSupervisor(SmsMessage smsMessage, Long userId) {
        try {
            // Buscar el usuario con su coordinador
            Optional<User> userOptional = userRepository.findByIdWithCoordinador(userId);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // Configurar el usuario
                smsMessage.setUser(user);
                smsMessage.setUserNombres(user.getNombres());

                // Verificar el rol y configurar el supervisor
                if (user.getCoordinador() != null) {
                    // Usuario tiene coordinador (supervisor)
                    smsMessage.setSupervisor(user.getCoordinador());
                    smsMessage.setSupervisorNombres(user.getCoordinador().getNombres());

                    logger.info("SMS registrado - Usuario: {} (ID: {}, Rol: {}), Supervisor: {} (ID: {})",
                            user.getNombres(), user.getId(), user.getRole(),
                            user.getCoordinador().getNombres(), user.getCoordinador().getId());
                } else {
                    // Usuario sin coordinador (puede ser coordinador, admin, etc.)
                    smsMessage.setSupervisor(null);
                    smsMessage.setSupervisorNombres("Sin coordinador");

                    logger.info("SMS registrado - Usuario: {} (ID: {}, Rol: {}), Sin coordinador asignado",
                            user.getNombres(), user.getId(), user.getRole());
                }
            } else {
                logger.warn("Usuario con ID {} no encontrado para registro de SMS", userId);
                // Configurar valores por defecto
                smsMessage.setUserNombres("Usuario no encontrado (ID: " + userId + ")");
                smsMessage.setSupervisorNombres("Sin coordinador");
            }

        } catch (Exception e) {
            logger.error("Error al configurar usuario y supervisor para userId {}: {}", userId, e.getMessage());
            // Configurar valores de error
            smsMessage.setUserNombres("Error al obtener usuario (ID: " + userId + ")");
            smsMessage.setSupervisorNombres("Sin coordinador");
        }
    }

    /**
     * Obtiene mensajes por usuario
     */
    public List<SmsMessage> getMessagesByUser(Long userId) {
        return smsMessageRepository.findAll().stream()
                .filter(msg -> msg.getUser() != null && msg.getUser().getId().equals(userId))
                .toList();
    }

    /**
     * Obtiene mensajes supervisados por un coordinador
     */
    public List<SmsMessage> getMessagesBySupervisor(Long supervisorId) {
        return smsMessageRepository.findAll().stream()
                .filter(msg -> msg.getSupervisor() != null && msg.getSupervisor().getId().equals(supervisorId))
                .toList();
    }

    /**
     * MÉTODO ESPECÍFICO PARA EL FRONTEND - Envía SMS con tracking automático
     */
    public SmsMessage enviarSmsDesdeFrontend(SmsEnvioFrontendDTO smsDTO) {
        try {
            // 1. Crear el código de tracking
            String codigoTracking = smsTrackingService.generarCodigoTracking(smsDTO.getUserId(), smsDTO.getNombreCliente());
            
            // 2. Generar la URL completa con tracking
            String urlCompleta = smsTrackingService.generarUrlCompleta(codigoTracking);
            
            // 3. Construir el mensaje final con la URL obligatoria
            String mensajeFinal = construirMensajeConUrl(smsDTO.getMensaje(), urlCompleta);
            
            // 4. Formatear número con prefijo de España (+34)
            String numeroCompleto = "+34" + smsDTO.getNumeroTelefono();
            
            // 5. Crear el mensaje SMS
            SmsMessage smsMessage = new SmsMessage();
            smsMessage.setFromNumber("SMS_CRM"); // Remitente por defecto
            smsMessage.setToNumber(numeroCompleto);
            smsMessage.setMessageText(mensajeFinal);
            smsMessage.setSendAt(smsDTO.getSendAt() != null ? smsDTO.getSendAt() : LocalDateTime.now());
            
            // 6. Configurar campos de tracking
            smsMessage.setCampania(smsDTO.getCampania());
            smsMessage.setTipoSms(smsDTO.getTipoSms());
            smsMessage.setNombreCliente(smsDTO.getNombreCliente());
            smsMessage.setCodigoTracking(codigoTracking);
            smsMessage.setUrlCompleta(urlCompleta);
            
            // 7. Configurar usuario y supervisor
            setupUserAndSupervisor(smsMessage, smsDTO.getUserId());
            
            // 8. Registrar IP
            smsMessage.setIpAddress(IpUtils.getClientIpAddress());
            
            // 9. Guardar el mensaje SMS
            smsMessage = smsMessageRepository.save(smsMessage);
            
            // 10. Crear el registro de tracking
            SmsTracking tracking = smsTrackingService.crearTracking(
                smsMessage, 
                smsDTO.getUserId(), 
                smsDTO.getNombreCliente(), 
                numeroCompleto
            );
            
            // 11. Procesar el envío inmediatamente
            smsMessage = processSingleMessage(smsMessage);
            
            logger.info("SMS enviado desde frontend - Usuario: {}, Cliente: {}, Código: {}", 
                       smsDTO.getUserId(), smsDTO.getNombreCliente(), codigoTracking);
            
            return smsMessage;
            
        } catch (Exception e) {
            logger.error("Error al enviar SMS desde frontend: ", e);
            throw new RuntimeException("Error al enviar SMS: " + e.getMessage());
        }
    }
    
    /**
     * Construye el mensaje final incluyendo la URL de tracking
     */
    private String construirMensajeConUrl(String mensajeOriginal, String urlCompleta) {
        // Verificar si el mensaje ya contiene una URL
        if (mensajeOriginal.toLowerCase().contains("http")) {
            // Si ya tiene URL, reemplazarla o agregarla al final
            return mensajeOriginal + "\n\nVisita: " + urlCompleta;
        } else {
            // Agregar la URL al final del mensaje
            return mensajeOriginal + "\n\nMás información: " + urlCompleta;
        }
    }
}