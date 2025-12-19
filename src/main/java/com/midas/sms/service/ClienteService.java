package com.midas.sms.service;

import com.midas.sms.dto.ClienteDTO;
import com.midas.sms.entity.Cliente;
import com.midas.sms.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteService {
    
    private final ClienteRepository clienteRepository;
    private final SmsService smsService;
    
    @Transactional
    public Cliente registrarCliente(ClienteDTO clienteDTO) {
        // Verificar si ya existe el documento
        if (clienteRepository.existsByNumeroDocumento(clienteDTO.getNumeroDocumento())) {
            throw new RuntimeException("Ya existe un cliente con este número de documento");
        }
        
        // Verificar si ya existe el celular
        if (clienteRepository.existsByNumeroCelular(clienteDTO.getNumeroCelular())) {
            throw new RuntimeException("Ya existe un cliente con este número de celular");
        }
        
        Cliente cliente = new Cliente();
        cliente.setTipoDocumento(clienteDTO.getTipoDocumento());
        cliente.setNumeroDocumento(clienteDTO.getNumeroDocumento());
        cliente.setApellidosNombres(clienteDTO.getApellidosNombres());
        cliente.setNumeroCelular(clienteDTO.getNumeroCelular());
        
        Cliente clienteGuardado = clienteRepository.save(cliente);
        
        // Enviar SMS de confirmación
        enviarSmsConfirmacion(clienteGuardado);
        
        return clienteGuardado;
    }
    
    /**
     * Envía un SMS de confirmación al cliente recién registrado
     */
    private void enviarSmsConfirmacion(Cliente cliente) {
        try {
            String mensaje = String.format(
                "Hola %s, tu registro fue exitoso. Gracias por registrarte.",
                cliente.getApellidosNombres()
            );
            
            smsService.sendSingleMessage(
                "MIDAS",                      // from
                cliente.getNumeroCelular(),   // to (ya incluye prefijo internacional)
                mensaje,                      // text
                null                          // sendAt (envío inmediato)
            );
            
            log.info("SMS de confirmación enviado a {} para cliente {}", 
                    cliente.getNumeroCelular(), cliente.getApellidosNombres());
                    
        } catch (Exception e) {
            // Log error pero no fallar el registro
            log.warn("No se pudo enviar SMS de confirmación al cliente {}: {}", 
                    cliente.getApellidosNombres(), e.getMessage());
        }
    }
    
    public List<Cliente> obtenerTodosLosClientes() {
        return clienteRepository.findAll();
    }
    
    /**
     * Obtener clientes con filtro opcional de fechas
     */
    public List<Cliente> obtenerClientes(String fechaDesde, String fechaHasta) {
        if (fechaDesde == null && fechaHasta == null) {
            return clienteRepository.findAll();
        }
        
        LocalDateTime desde = null;
        LocalDateTime hasta = null;
        
        if (fechaDesde != null && !fechaDesde.isEmpty()) {
            desde = LocalDate.parse(fechaDesde).atStartOfDay();
        }
        
        if (fechaHasta != null && !fechaHasta.isEmpty()) {
            hasta = LocalDate.parse(fechaHasta).atTime(LocalTime.MAX);
        }
        
        if (desde != null && hasta != null) {
            return clienteRepository.findByFechaRegistroBetween(desde, hasta);
        } else if (desde != null) {
            return clienteRepository.findByFechaRegistroAfter(desde);
        } else {
            return clienteRepository.findByFechaRegistroBefore(hasta);
        }
    }
    
    public Optional<Cliente> obtenerClientePorDocumento(String numeroDocumento) {
        return clienteRepository.findByNumeroDocumento(numeroDocumento);
    }
    
    public long contarClientes() {
        return clienteRepository.count();
    }
}