package com.midas.sms.repository;

import com.midas.sms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Buscar usuario por ID
    Optional<User> findById(Long id);
    
    // Buscar usuario por username
    Optional<User> findByUsername(String username);
    
    // Buscar usuario por DNI
    Optional<User> findByDni(String dni);
    
    // Buscar usuario por email
    Optional<User> findByEmail(String email);
    
    // Obtener usuario con su coordinador (supervisor)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.coordinador WHERE u.id = :userId")
    Optional<User> findByIdWithCoordinador(@Param("userId") Long userId);
    
    // Verificar si un usuario existe y está activo
    @Query("SELECT u FROM User u WHERE u.id = :userId AND u.estado = 'A'")
    Optional<User> findActiveUserById(@Param("userId") Long userId);
    
    // Obtener nombres completos del usuario
    @Query("SELECT CONCAT(u.nombre, ' ', u.apellido) FROM User u WHERE u.id = :userId")
    Optional<String> findNombresById(@Param("userId") Long userId);
    
    // Obtener coordinador de un usuario
    @Query("SELECT u.coordinador FROM User u WHERE u.id = :userId")
    Optional<User> findCoordinadorByUserId(@Param("userId") Long userId);
}