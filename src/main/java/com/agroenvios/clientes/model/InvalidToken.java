package com.agroenvios.clientes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invalid_tokens")
public class InvalidToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Date expirationDate;

    @Column(nullable = false)
    private Date invalidationDate;

    @Column(length = 50)
    private String username;

    public InvalidToken(String token, Date expirationDate, String username) {
        this.token = token;
        this.expirationDate = expirationDate;
        this.invalidationDate = new Date();
        this.username = username;
    }
}
