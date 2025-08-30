package com.midas.sms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private String tipoDocumento;
    
    @Column(name = "numero_documento", nullable = false, unique = true, length = 20)
    private String numeroDocumento;
    
    @Column(name = "apellidos_nombres", nullable = false, length = 100)
    private String apellidosNombres;
    
    @Column(name = "numero_celular", nullable = false, length = 20)
    private String numeroCelular;
    
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
    
    @PrePersist
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
    }
}