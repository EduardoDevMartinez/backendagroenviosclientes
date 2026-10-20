package com.agroenvios.clientes.primary.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user", uniqueConstraints = {@UniqueConstraint(columnNames = {"username"}), @UniqueConstraint(columnNames = {"correo"}), @UniqueConstraint(columnNames = {"telefono"})})
public class User extends BaseEntity implements UserDetails{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String paterno;

    @Column(nullable = false)
    private String materno;

    @Column(nullable = false)
    private String correo;

    @Column(nullable = false)
    private String telefono;

    private String foto;

    private String pushToken;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isEmailVerified= false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isTelefonoVerified= false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    /**
     * Mayoreo fijo (lo activa el admin): el cliente ve y paga el precio de mayoreo en todos los
     * productos que lo tengan, sin importar la cantidad. Nulo cuenta como desactivado (la columna
     * la agrega hbm2ddl sin valor en los clientes existentes).
     */
    private Boolean mayoreoFijo;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return Boolean.TRUE.equals(isActive);
    }

    @Override
    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(isActive);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return Boolean.TRUE.equals(isActive);
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive);
    }


}
