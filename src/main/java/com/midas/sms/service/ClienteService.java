package com.midas.sms.service;

import com.midas.sms.dto.ClienteDTO;
import com.midas.sms.entity.Cliente;
import com.midas.sms.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    public Optional<Cliente> obtenerClientePorDocumento(String numeroDocumento) {
        return clienteRepository.findByNumeroDocumento(numeroDocumento);
    }
    
    public long contarClientes() {
        return clienteRepository.count();
    }
}