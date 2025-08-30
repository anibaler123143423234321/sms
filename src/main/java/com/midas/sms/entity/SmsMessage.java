package com.midas.sms.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_messages")
public class SmsMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    @Column(name = "from_number", length = 255)
    private String fromNumber;

    @Column(name = "gateway_response", length = 500)
    private String gatewayResponse;

    @Column(name = "send_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime sendAt;

    @Column(name = "sent_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MessageStatus status;

    @Column(name = "message_text", length = 1000)
    private String messageText;

    @Column(name = "to_number", length = 255)
    private String toNumber;

    // Nuevos campos para supervisión
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private User user;

    @Column(name = "user_nombres", length = 200)
    private String userNombres;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private User supervisor;

    @Column(name = "supervisor_nombres", length = 200)
    private String supervisorNombres;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // Nuevos campos para el sistema de tracking
    @Column(name = "campania", length = 100)
    private String campania;

    @Column(name = "tipo_sms", length = 100)
    private String tipoSms;

    @Column(name = "nombre_cliente", length = 200)
    private String nombreCliente;

    @Column(name = "codigo_tracking", unique = true, length = 50)
    private String codigoTracking;

    @Column(name = "url_completa", length = 500)
    private String urlCompleta;

    public enum MessageStatus {
        DELIVERED, ERROR, FAILED, PENDING, SENT
    }

    // Constructor por defecto
    public SmsMessage() {
        this.createdAt = LocalDateTime.now();
        this.status = MessageStatus.PENDING;
    }

    // Constructor con parámetros principales
    public SmsMessage(String fromNumber, String toNumber, String messageText, LocalDateTime sendAt) {
        this();
        this.fromNumber = fromNumber;
        this.toNumber = toNumber;
        this.messageText = messageText;
        this.sendAt = sendAt;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }

    public LocalDateTime getSendAt() {
        return sendAt;
    }

    public void setSendAt(LocalDateTime sendAt) {
        this.sendAt = sendAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getToNumber() {
        return toNumber;
    }

    public void setToNumber(String toNumber) {
        this.toNumber = toNumber;
    }

    // Getters y Setters para los nuevos campos
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        // Actualizar automáticamente el nombre del usuario
        if (user != null) {
            this.userNombres = user.getNombres();
        }
    }

    public String getUserNombres() {
        return userNombres;
    }

    public void setUserNombres(String userNombres) {
        this.userNombres = userNombres;
    }

    public User getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(User supervisor) {
        this.supervisor = supervisor;
        // Actualizar automáticamente el nombre del supervisor
        if (supervisor != null) {
            this.supervisorNombres = supervisor.getNombres();
        }
    }

    public String getSupervisorNombres() {
        return supervisorNombres;
    }

    public void setSupervisorNombres(String supervisorNombres) {
        this.supervisorNombres = supervisorNombres;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    // Getters y Setters para los campos de tracking
    public String getCampania() {
        return campania;
    }

    public void setCampania(String campania) {
        this.campania = campania;
    }

    public String getTipoSms() {
        return tipoSms;
    }

    public void setTipoSms(String tipoSms) {
        this.tipoSms = tipoSms;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getCodigoTracking() {
        return codigoTracking;
    }

    public void setCodigoTracking(String codigoTracking) {
        this.codigoTracking = codigoTracking;
    }

    public String getUrlCompleta() {
        return urlCompleta;
    }

    public void setUrlCompleta(String urlCompleta) {
        this.urlCompleta = urlCompleta;
    }
}