package com.midas.sms.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_tracking")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsTracking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "codigo_tracking", unique = true, nullable = false, length = 50)
    private String codigoTracking;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sms_message_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private SmsMessage smsMessage;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "nombre_cliente", length = 200)
    private String nombreCliente;
    
    @Column(name = "numero_telefono", length = 20)
    private String numeroTelefono;
    
    @Column(name = "fecha_envio")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime fechaEnvio;
    
    @Column(name = "fecha_visita")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime fechaVisita;
    
    @Column(name = "ip_visita", length = 45)
    private String ipVisita;
    
    @Column(name = "visitado", nullable = false)
    private Boolean visitado = false;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (visitado == null) {
            visitado = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Método para marcar como visitado
    public void marcarComoVisitado(String ipVisita, String userAgent) {
        this.visitado = true;
        this.fechaVisita = LocalDateTime.now();
        this.ipVisita = ipVisita;
        this.userAgent = userAgent;
        this.updatedAt = LocalDateTime.now();
    }
}