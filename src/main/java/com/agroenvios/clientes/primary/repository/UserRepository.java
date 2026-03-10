package com.agroenvios.clientes.primary.repository;

import com.agroenvios.clientes.primary.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);
    Optional<User> findByCorreo(String correo);
    boolean existsByUsername(String username);
    boolean existsByCorreo(String correo);
    boolean existsByTelefono(String telefono);

}
