package com.midas.sms.controller;

import com.midas.sms.dto.ApiResponse;
import com.midas.sms.dto.ClienteDTO;
import com.midas.sms.entity.Cliente;
import com.midas.sms.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4321", "http://localhost:3000"})
public class ClienteController {
    
    private final ClienteService clienteService;
    
    @PostMapping("/registrar")
    public ResponseEntity<ApiResponse<Cliente>> registrarCliente(
            @Valid @RequestBody ClienteDTO clienteDTO,
            BindingResult bindingResult) {
        
        try {
            // Validar errores de validación
            if (bindingResult.hasErrors()) {
                String errores = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Errores de validación: " + errores));
            }
            
            Cliente cliente = clienteService.registrarCliente(clienteDTO);
            return ResponseEntity.ok(
                    ApiResponse.success("Cliente registrado exitosamente", cliente)
            );
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno del servidor"));
        }
    }
    
    @GetMapping("/listar")
    public ResponseEntity<ApiResponse<List<Cliente>>> listarClientes() {
        try {
            List<Cliente> clientes = clienteService.obtenerTodosLosClientes();
            return ResponseEntity.ok(
                    ApiResponse.success("Clientes obtenidos exitosamente", clientes)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener los clientes"));
        }
    }
    
    @GetMapping("/buscar/{numeroDocumento}")
    public ResponseEntity<ApiResponse<Cliente>> buscarCliente(
            @PathVariable String numeroDocumento) {
        try {
            return clienteService.obtenerClientePorDocumento(numeroDocumento)
                    .map(cliente -> ResponseEntity.ok(
                            ApiResponse.success("Cliente encontrado", cliente)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al buscar el cliente"));
        }
    }
    
    @GetMapping("/contar")
    public ResponseEntity<ApiResponse<Long>> contarClientes() {
        try {
            long count = clienteService.contarClientes();
            return ResponseEntity.ok(
                    ApiResponse.success("Conteo obtenido exitosamente", count)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al contar los clientes"));
        }
    }
}