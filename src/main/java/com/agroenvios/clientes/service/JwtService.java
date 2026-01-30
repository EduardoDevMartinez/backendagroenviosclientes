package com.agroenvios.clientes.service;

import com.agroenvios.clientes.repository.InvalidTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${secret.key.jwt}")
    private String key;
    private Key SECRET_KEY;

    private final InvalidTokenRepository invalidTokenRepository;

    @PostConstruct
    public void init() {
        SECRET_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(key));
    }

    public String getToken(UserDetails user) {
        return generateToken(new HashMap<>(), user);
    }

    private String generateToken(Map<String, Object> extraClaims, UserDetails user) {

        long expirationTimeMillis = System.currentTimeMillis() + (1000L * 60 * 60 * 8);
        Date expirationDate = new Date(expirationTimeMillis);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expirationDate)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (isTokenInvalidated(token)) {
            return false;
        }
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenInvalidated(String token) {
        return invalidTokenRepository.existsByToken(token);
    }


    public String generateValidateUsernameToken(String username) {

        long expirationTimeMillis = System.currentTimeMillis() + 1000L * 60 * 60 * 24; // 24 horas
        Date expirationDate = new Date(expirationTimeMillis);

        Map<String, Object> claims = new HashMap<>();
        claims.put("validateEmail", true);
        claims.put("tokenType", "EMAIL_VALIDATION");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expirationDate)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }


    public boolean isEmailValidationToken(String token) {
        try {
            Claims claims = getAllClaims(token);
            return claims.get("validateEmail", Boolean.class) != null &&
                    claims.get("validateEmail", Boolean.class) &&
                    !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }


    public String getUsernameFromToken(String token) {
        return getClaim(token, Claims::getSubject);
    }


    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Date getExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return getExpiration(token).before(new Date());
    }

    public String generatePasswordResetToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("purpose", "PASSWORD_RESET");

        long expirationTimeMillis = System.currentTimeMillis() + (1000L * 60 * 15);
        Date expirationDate = new Date(expirationTimeMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expirationDate)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isPasswordResetToken(String token) {
        try {
            Claims claims = getAllClaims(token);
            return "PASSWORD_RESET".equals(claims.get("purpose")) &&
                    !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
