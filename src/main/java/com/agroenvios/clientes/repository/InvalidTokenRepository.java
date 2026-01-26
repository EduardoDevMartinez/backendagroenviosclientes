package com.agroenvios.clientes.repository;

import com.agroenvios.clientes.model.InvalidToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface InvalidTokenRepository extends JpaRepository<InvalidToken, Integer> {
    boolean existsByToken(String token);


    @Query("DELETE FROM InvalidToken t WHERE t.expirationDate < :now")
    @Modifying
    void deleteExpiredTokens(@Param("now") Date now);
}
