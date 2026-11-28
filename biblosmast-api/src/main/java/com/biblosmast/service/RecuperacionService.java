package com.biblosmast.service;

import com.biblosmast.model.entity.Cuenta;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.model.entity.RecuperacionClave;
import com.biblosmast.repository.CuentaRepository;
import com.biblosmast.repository.RecuperacionClaveRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RecuperacionService implements IRecuperacionService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionService.class);
    private static final String ENLACE_INVALIDO = "El enlace no es válido o ya venció";

    private final CuentaRepository cuentaRepository;
    private final RecuperacionClaveRepository recuperacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ICorreoService correoService;
    private final IAuditoriaService auditoriaService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String urlFrontend;

    @Value("${recuperacion.minutos-validez:30}")
    private int minutosValidez;

    @Override
    @Transactional
    public void solicitar(String correo) {
        Cuenta cuenta = cuentaRepository.buscarPorUsernameOCorreo(correo.trim()).orElse(null);
        if (cuenta == null || !Boolean.TRUE.equals(cuenta.getActivo())
                || cuenta.getUsuario().getEstado() == EstadoUsuario.INACTIVO) {
            return; // misma respuesta que si existiera: no se revela qué correos están registrados
        }

        LocalDateTime ahora = LocalDateTime.now();
        // Máximo un enlace por minuto por cuenta (evita llenar un buzón ajeno de correos)
        if (recuperacionRepository.existsByCuentaIdCuentaAndCreadaEnAfter(cuenta.getIdCuenta(), ahora.minusMinutes(1))) {
            return;
        }
        // Los enlaces anteriores sin usar dejan de servir
        for (RecuperacionClave anterior : recuperacionRepository.findByCuentaIdCuentaAndUsadaEnIsNull(cuenta.getIdCuenta())) {
            anterior.setUsadaEn(ahora);
        }

        String token = generarToken();
        RecuperacionClave recuperacion = new RecuperacionClave();
        recuperacion.setCuenta(cuenta);
        recuperacion.setTokenHash(hash(token));
        recuperacion.setCreadaEn(ahora);
        recuperacion.setExpiraEn(ahora.plusMinutes(minutosValidez));
        recuperacionRepository.save(recuperacion);

        String enlace = urlFrontend + "/restablecer.html?token=" + token;
        correoService.enviar(cuenta.getUsuario().getEmail(), "Recupera tu contraseña de Biblosmast",
                "Hola " + cuenta.getUsuario().getNombres() + ",\n\n"
                        + "Recibimos una solicitud para cambiar la contraseña de tu cuenta '" + cuenta.getUsername() + "'.\n"
                        + "Abre este enlace para elegir una nueva (vale " + minutosValidez + " minutos y solo se puede usar una vez):\n\n"
                        + enlace + "\n\n"
                        + "Si no fuiste tú, ignora este mensaje: tu contraseña no cambia.");
        auditar(cuenta, "RECUPERACION_SOLICITADA", "Se envió un enlace de recuperación a la cuenta '" + cuenta.getUsername() + "'");
    }

    @Override
    @Transactional
    public void restablecer(String token, String claveNueva) {
        RecuperacionClave recuperacion = recuperacionRepository.findByTokenHash(hash(token.trim())).orElse(null);
        LocalDateTime ahora = LocalDateTime.now();
        if (recuperacion == null || recuperacion.getUsadaEn() != null || ahora.isAfter(recuperacion.getExpiraEn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ENLACE_INVALIDO);
        }
        Cuenta cuenta = recuperacion.getCuenta();
        if (Boolean.TRUE.equals(cuenta.getEliminada()) || !Boolean.TRUE.equals(cuenta.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ENLACE_INVALIDO);
        }

        cuenta.setPasswordHash(passwordEncoder.encode(claveNueva));
        cuenta.setDebeCambiarClave(false);
        cuenta.setIntentosFallidos(0);
        cuenta.setBloqueadoHasta(null);
        cuenta.setTokenDesde(ahora.truncatedTo(ChronoUnit.SECONDS)); // cierra las sesiones abiertas
        cuentaRepository.save(cuenta);

        recuperacion.setUsadaEn(ahora);
        recuperacionRepository.save(recuperacion);
        auditar(cuenta, "CLAVE_RECUPERADA", "La cuenta '" + cuenta.getUsername() + "' cambió su contraseña con un enlace de recuperación");
    }

    /** 32 bytes al azar en Base64URL: imposible de adivinar. */
    private String generarToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal: en la base de datos solo se guarda este hash. */
    private String hash(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : resumen) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    private void auditar(Cuenta cuenta, String accion, String detalle) {
        try {
            auditoriaService.registrar(cuenta.getUsuario().getIdUsuario(), accion, "cuenta", cuenta.getIdCuenta(), detalle);
        } catch (Exception e) {
            log.warn("No se pudo registrar la auditoría ({}): {}", accion, e.getMessage());
        }
    }
}
