package com.midas.sms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RoleAuthorizationInterceptor roleAuthorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleAuthorizationInterceptor)
                .addPathPatterns("/api/sms/**")
                .excludePathPatterns(
                        "/api/sms/tracking/**", // Permitir tracking desde landing sin autenticación
                        "/api/sms/messages", // Permitir consulta de mensajes
                        "/api/sms/messages/**" // Permitir consulta específica
                );
    }
}