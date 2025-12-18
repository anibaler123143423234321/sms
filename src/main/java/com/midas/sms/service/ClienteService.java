package com.midas.sms.service;

import com.midas.sms.dto.ClienteDTO;
import com.midas.sms.entity.Cliente;
import com.midas.sms.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClienteService {
    
    private final ClienteRepository clienteRepository;
    
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
        
        return clienteRepository.save(cliente);
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