package com.midas.sms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;

public class SmsMessageDTO {
    
    @JsonProperty("from")
    private String from;
    
    @JsonProperty("to")
    private String to;
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("send_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime sendAt;
    
    // Constructor por defecto
    public SmsMessageDTO() {}
    
    // Constructor con parámetros
    public SmsMessageDTO(String from, String to, String text, LocalDateTime sendAt) {
        this.from = from;
        this.to = to;
        this.text = text;
        this.sendAt = sendAt;
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
}