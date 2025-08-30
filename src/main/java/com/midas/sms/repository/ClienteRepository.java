package com.midas.sms.repository;

import com.midas.sms.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);
    
    boolean existsByNumeroDocumento(String numeroDocumento);
    
    boolean existsByNumeroCelular(String numeroCelular);
}