package com.biblosmast.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Crea y valida los tokens JWT.
 * El token se firma con HMAC-SHA256 (HS256): cualquier cambio en su contenido invalida la firma.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int BYTES_MINIMOS = 32; // 256 bits, lo que exige HS256

    private final SecretKey clave;
    private final long duracionMs;

    public JwtService(@Value("${jwt.secret:}") String secreto,
                      @Value("${jwt.expiracion-minutos:60}") long expiracionMinutos) {
        if (secreto == null || secreto.isBlank()) {
            byte[] aleatorio = new byte[BYTES_MINIMOS];
            new SecureRandom().nextBytes(aleatorio);
            this.clave = new SecretKeySpec(aleatorio, SignatureAlgorithm.HS256.getJcaName());
            log.warn("jwt.secret no está definido: se generó una clave al azar y los tokens dejarán de servir "
                    + "al reiniciar la API. Define la variable de entorno JWT_SECRET (mínimo 32 caracteres).");
        } else {
            byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < BYTES_MINIMOS) {
                throw new IllegalStateException("jwt.secret debe tener al menos 32 caracteres (256 bits) para HS256");
            }
            this.clave = new SecretKeySpec(bytes, SignatureAlgorithm.HS256.getJcaName());
        }
        if (expiracionMinutos <= 0) {
            throw new IllegalStateException("jwt.expiracion-minutos debe ser mayor que 0");
        }
        this.duracionMs = Duration.ofMinutes(expiracionMinutos).toMillis();
    }

    /** Crea un token con el usuario (sub), su id (uid), su rol (rol), la fecha de emisión (iat) y la de vencimiento (exp). */
    public TokenGenerado generarToken(Integer idUsuario, String username, String rol) {
        Date ahora = new Date();
        Date vencimiento = new Date(ahora.getTime() + duracionMs);
        String token = Jwts.builder()
                .setSubject(username)
                .claim("uid", idUsuario)
                .claim("rol", rol)
                .setIssuedAt(ahora)
                .setExpiration(vencimiento)
                .signWith(clave, SignatureAlgorithm.HS256)
                .compact();
        return new TokenGenerado(token, vencimiento.getTime() / 1000);
    }

    /**
     * Comprueba la firma y el vencimiento y devuelve el contenido (claims) del token.
     * Lanza JwtException si el token está alterado, mal formado o vencido (ExpiredJwtException).
     */
    public Claims validar(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(clave)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
