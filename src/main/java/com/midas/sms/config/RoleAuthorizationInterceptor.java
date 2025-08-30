package com.midas.sms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.midas.sms.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    private static final List<String> ALLOWED_ROLES = Arrays.asList(
        "ADMIN", 
        "PROGRAMADOR", 
        "ASESOR", 
        "SUPERVISOR", 
        "JEFE_PISO", 
        "BACKOFFICE"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // Solo aplicar autorización a endpoints SMS específicos
        String requestURI = request.getRequestURI();
        if (!requestURI.startsWith("/api/sms/")) {
            return true; // Permitir otras rutas
        }

        // Permitir endpoints de tracking sin autenticación (para landing page)
        if (requestURI.startsWith("/api/sms/tracking/")) {
            return true;
        }

        // Obtener el token del header Authorization
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(response, "Token de autorización no proporcionado");
            return false;
        }

        // Extraer el rol del JWT (implementación simple)
        String userRole = extractRoleFromJWT(authHeader);
        
        if (userRole == null || userRole.trim().isEmpty()) {
            sendUnauthorizedResponse(response, "No se pudo extraer el rol del token");
            return false;
        }

        // Verificar si el rol está en la lista de roles permitidos
        if (!ALLOWED_ROLES.contains(userRole.toUpperCase())) {
            sendUnauthorizedResponse(response, "Rol '" + userRole + "' no autorizado para usar SMS");
            return false;
        }

        return true; // Autorizado
    }

    /**
     * Extrae el rol del usuario desde el token JWT
     */
    private String extractRoleFromJWT(String authHeader) {
        try {
            // Remover "Bearer " del token
            String jwtToken = authHeader.substring(7);
            
            // Dividir el JWT en sus partes
            String[] chunks = jwtToken.split("\\.");
            if (chunks.length != 3) {
                return null;
            }

            // Decodificar el payload (segunda parte)
            String payload = chunks[1];
            
            // Agregar padding si es necesario para Base64
            while (payload.length() % 4 != 0) {
                payload += "=";
            }
            
            // Decodificar Base64
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);
            
            // Parsear JSON para extraer el rol
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(decodedPayload);
            
            // Buscar el rol en diferentes campos posibles
            if (jsonNode.has("roles")) {
                String roles = jsonNode.get("roles").asText();
                // Si roles es algo como "ROLE_ADMIN", extraer solo "ADMIN"
                if (roles.startsWith("ROLE_")) {
                    return roles.substring(5);
                }
                return roles;
            }
            
            if (jsonNode.has("role")) {
                return jsonNode.get("role").asText();
            }
            
            if (jsonNode.has("authorities")) {
                com.fasterxml.jackson.databind.JsonNode authorities = jsonNode.get("authorities");
                if (authorities.isArray() && authorities.size() > 0) {
                    String authority = authorities.get(0).asText();
                    if (authority.startsWith("ROLE_")) {
                        return authority.substring(5);
                    }
                    return authority;
                }
            }
            
            return null;
            
        } catch (Exception e) {
            // Log del error pero no fallar la aplicación
            System.err.println("Error al decodificar JWT: " + e.getMessage());
            return null;
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        ApiResponse<String> errorResponse = new ApiResponse<>(false, message, null);
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        
        response.getWriter().write(jsonResponse);
    }
}