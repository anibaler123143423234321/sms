package com.midas.sms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class SmsMessageWithUserDTO {
    
    private String from;
    private String to;
    private String text;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendAt;
    
    private Long userId;
    
    // Constructor por defecto
    public SmsMessageWithUserDTO() {}
    
    // Constructor con parámetros
    public SmsMessageWithUserDTO(String from, String to, String text, LocalDateTime sendAt, Long userId) {
        this.from = from;
        this.to = to;
        this.text = text;
        this.sendAt = sendAt;
        this.userId = userId;
    }
    
    // Getters y Setters
    public String getFrom() {
        return from;
    }
    
    public void setFrom(String from) {
        this.from = from;
    }
    
    public String getTo() {
        return to;
    }
    
    public void setTo(String to) {
        this.to = to;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public LocalDateTime getSendAt() {
        return sendAt;
    }
    
    public void setSendAt(LocalDateTime sendAt) {
        this.sendAt = sendAt;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}