package com.midas.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuscarAudiosVentaResponse {
    private boolean success;
    private String message;
    private List<AudioVentaDTO> audios;
    private int totalAudios;
}

