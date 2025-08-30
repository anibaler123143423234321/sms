package com.midas.sms.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "usuarios")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codi_usuario")
    private Long id;

    @Column(name = "usuario", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "clave", nullable = false, length = 200)
    private String password;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "apellido", nullable = false)
    private String apellido;

    @Column(name = "dni", nullable = false, length = 8, unique = true)
    private String dni;

    @Column(name = "telefono", nullable = true)
    private String telefono;

    @Column(name = "email", nullable = true, length = 50, unique = true)
    private String email;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_cese", nullable = true)
    private LocalDate fechaCese;

    @Column(name = "estado", nullable = false, length = 1)
    private String estado = "A"; // Valor por defecto

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name="foto", length = 2000, nullable = true )
    private String picture;

    @Transient
    private String token;

    @JsonProperty("tokenPassword")
    @Column(name = "tokenPassword", nullable = true)
    private String tokenPassword;

    @Column(name = "deletion_time", nullable = true)
    private LocalDateTime deletionTime;

    // Mantenemos la columna sede como String para compatibilidad con datos existentes
    @Column(name = "sede", nullable = true, length = 100)
    private String sedeNombre;

    // Nueva relación con la entidad Sede
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Sede sede;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinador_id")
    @JsonBackReference  // Evita la serialización inversa para evitar ciclo
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User coordinador;

    @OneToMany(mappedBy = "coordinador", fetch = FetchType.LAZY)
    @JsonManagedReference  // Serializa esta colección sin volver a serializar el coordinador
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<User> asesores;

    // --- NUEVOS CAMPOS PARA CREDENCIALES DE GOOGLE ---
    @Lob // Large Object: para textos largos como los tokens
    @Column(name = "google_access_token", columnDefinition = "TEXT", nullable = true)
    private String googleAccessToken;

    @Lob
    @Column(name = "google_refresh_token", columnDefinition = "TEXT", nullable = true)
    private String googleRefreshToken;

    @Column(name = "google_token_expiry_time", nullable = true)
    private Long googleTokenExpiryTime; // Guardaremos la fecha de expiración en milisegundos

    // --- NUEVO CAMPO PARA NÚMERO DE SERVIDOR DEL COORDINADOR ---
    @Column(name = "numero_servidor", nullable = true, length = 10)
    private String numeroServidor; // Número de servidor para navegación en Google Drive

    @Column(name = "google_account_email", nullable = true)
    private String googleAccountEmail; // El email de la cuenta de Google conectada
    // --- FIN DE NUEVOS CAMPOS ---

    // Dentro de la clase com.midas.crm.entity.User
    public User(Long id) {
        this.id = id;
    }

    public String getNombres() {
        return this.nombre + " " + this.apellido;
    }

}
