package com.biblosmast.service;

import com.biblosmast.model.dto.LoginRequest;
import com.biblosmast.model.dto.LoginResponse;
import com.biblosmast.model.entity.Cuenta;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.repository.CuentaRepository;
import com.biblosmast.security.JwtService;
import com.biblosmast.security.TokenGenerado;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
public class AuthService implements IAuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String MENSAJE_INVALIDO = "Usuario o contraseña incorrectos";
    // Hash BCrypt de relleno: se compara igual cuando el usuario no existe,
    // para que el tiempo de respuesta no revele si el usuario existe o no.
    private static final String HASH_RELLENO = "$2a$10$nIrHXnZljgQngFyUUhpJpekfHxrNjBBOMiASF.2GKC0l/CTBeyyni";

    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IAuditoriaService auditoriaService;

    /** Claves incorrectas seguidas que bloquean la cuenta. */
    @Value("${seguridad.max-intentos:5}")
    private int maxIntentos;

    /** Cuánto dura el bloqueo. */
    @Value("${seguridad.bloqueo-minutos:15}")
    private int bloqueoMinutos;

    /**
     * noRollbackFor: cuando el login falla se lanza una excepción, pero el contador de intentos
     * fallidos y el bloqueo sí deben quedar guardados.
     */
    @Override
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        // Se puede entrar con el nombre de usuario o con el correo
        Cuenta cuenta = cuentaRepository.buscarPorUsernameOCorreo(username).orElse(null);
        LocalDateTime ahora = LocalDateTime.now();

        // 1) Cuenta bloqueada por intentos fallidos
        if (cuenta != null && cuenta.getBloqueadoHasta() != null && ahora.isBefore(cuenta.getBloqueadoHasta())) {
            long minutos = Math.max(1, Duration.between(ahora, cuenta.getBloqueadoHasta()).toMinutes() + 1);
            auditar(cuenta.getUsuario().getIdUsuario(), "LOGIN_BLOQUEADO", cuenta.getIdCuenta(),
                    "Intento con la cuenta bloqueada: '" + cuenta.getUsername() + "'");
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "La cuenta está bloqueada por demasiados intentos fallidos. Intenta de nuevo en "
                            + minutos + (minutos == 1 ? " minuto" : " minutos"));
        }

        // 2) Contraseña
        String hash = (cuenta != null) ? cuenta.getPasswordHash() : HASH_RELLENO;
        boolean passwordOk = passwordEncoder.matches(request.getPassword(), hash);
        if (cuenta == null || !passwordOk) {
            throw registrarFallo(cuenta, username, ahora);
        }

        // 3) Cuenta inactiva
        if (!Boolean.TRUE.equals(cuenta.getActivo())
                || cuenta.getUsuario().getEstado() == EstadoUsuario.INACTIVO) {
            auditar(cuenta.getUsuario().getIdUsuario(), "LOGIN_FALLIDO", cuenta.getIdCuenta(),
                    "Cuenta inactiva: '" + cuenta.getUsername() + "'");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta está inactiva");
        }

        // 4) Todo bien: se reinicia el contador y se crea el token
        cuenta.setIntentosFallidos(0);
        cuenta.setBloqueadoHasta(null);
        cuentaRepository.save(cuenta);
        auditar(cuenta.getUsuario().getIdUsuario(), "LOGIN", cuenta.getIdCuenta(),
                "Inicio de sesión: '" + cuenta.getUsername() + "'");
        return emitirSesion(cuenta);
    }

    /** Suma un intento fallido; al llegar al máximo bloquea la cuenta. Devuelve el error que debe lanzarse. */
    private ResponseStatusException registrarFallo(Cuenta cuenta, String username, LocalDateTime ahora) {
        if (cuenta != null) {
            int intentos = cuenta.getIntentosFallidos() + 1;
            if (intentos >= maxIntentos) {
                cuenta.setIntentosFallidos(0);
                cuenta.setBloqueadoHasta(ahora.plusMinutes(bloqueoMinutos));
                cuentaRepository.save(cuenta);
                auditar(cuenta.getUsuario().getIdUsuario(), "LOGIN_BLOQUEADO", cuenta.getIdCuenta(),
                        "Cuenta '" + cuenta.getUsername() + "' bloqueada " + bloqueoMinutos + " min tras " + maxIntentos + " intentos fallidos");
                return new ResponseStatusException(HttpStatus.LOCKED,
                        "Demasiados intentos fallidos: la cuenta quedó bloqueada por " + bloqueoMinutos + " minutos");
            }
            cuenta.setIntentosFallidos(intentos);
            cuentaRepository.save(cuenta);
        }
        auditar(cuenta == null ? null : cuenta.getUsuario().getIdUsuario(), "LOGIN_FALLIDO",
                cuenta == null ? null : cuenta.getIdCuenta(),
                "Intento fallido con el usuario '" + recortar(username) + "'");
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, MENSAJE_INVALIDO);
    }

    @Override
    @Transactional
    public void logout(String username) {
        Cuenta cuenta = cuentaRepository.buscarPorUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida"));
        // Truncado a segundos: así MySQL no lo redondea hacia arriba y un login inmediato no queda invalidado
        cuenta.setTokenDesde(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        cuentaRepository.save(cuenta);
        auditar(cuenta.getUsuario().getIdUsuario(), "LOGOUT", cuenta.getIdCuenta(),
                "Cierre de sesión: '" + cuenta.getUsername() + "'");
    }

    @Override
    public LoginResponse emitirSesion(Cuenta cuenta) {
        TokenGenerado token = jwtService.generarToken(
                cuenta.getUsuario().getIdUsuario(), cuenta.getUsername(), cuenta.getRol().getNombre());
        return new LoginResponse(
                cuenta.getUsuario().getIdUsuario(),
                cuenta.getUsername(),
                cuenta.getUsuario().getNombres(),
                cuenta.getUsuario().getApellidos(),
                cuenta.getRol().getNombre(),
                token.token(),
                "Bearer",
                token.expiraEnSegundos(),
                Boolean.TRUE.equals(cuenta.getDebeCambiarClave()));
    }

    private String recortar(String texto) {
        return texto.length() <= 40 ? texto : texto.substring(0, 40);
    }

    /** Auditar nunca debe impedir (ni cambiar) la respuesta del login. */
    private void auditar(Integer idUsuario, String accion, Integer idCuenta, String detalle) {
        try {
            auditoriaService.registrar(idUsuario, accion, "cuenta", idCuenta, detalle);
        } catch (Exception e) {
            log.warn("No se pudo registrar la auditoría ({}): {}", accion, e.getMessage());
        }
    }
}
