package com.midas.sms.repository;

import com.midas.sms.entity.ServidorCixVidarte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServidorCixVidarteRepository extends JpaRepository<ServidorCixVidarte, Long> {
}