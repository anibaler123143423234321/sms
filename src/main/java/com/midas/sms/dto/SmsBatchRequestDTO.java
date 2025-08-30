package com.midas.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SmsBatchRequestDTO {

    @JsonProperty("api_key")
    private String apiKey;

    @JsonProperty("report_url")
    private String reportUrl;

    @JsonProperty("concat")
    private Integer concat;

    @JsonProperty("messages")
    private List<SmsMessageDTO> messages;

    // Constructor por defecto
    public SmsBatchRequestDTO() {
    }

    // Constructor con parámetros
    public SmsBatchRequestDTO(String apiKey, String reportUrl, Integer concat, List<SmsMessageDTO> messages) {
        this.apiKey = apiKey;
        this.reportUrl = reportUrl;
        this.concat = concat;
        this.messages = messages;
    }

    // Getters y Setters
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public Integer getConcat() {
        return concat;
    }

    public void setConcat(Integer concat) {
        this.concat = concat;
    }

    public List<SmsMessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<SmsMessageDTO> messages) {
        this.messages = messages;
    }
}