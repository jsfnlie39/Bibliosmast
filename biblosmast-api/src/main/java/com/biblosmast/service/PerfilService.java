package com.biblosmast.service;

import com.biblosmast.model.dto.CambioClaveRequest;
import com.biblosmast.model.dto.LoginResponse;
import com.biblosmast.model.dto.PerfilRequest;
import com.biblosmast.model.dto.PerfilResponse;
import com.biblosmast.model.entity.Cuenta;
import com.biblosmast.model.entity.Usuario;
import com.biblosmast.repository.CuentaRepository;
import com.biblosmast.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PerfilService implements IPerfilService {

    private static final Logger log = LoggerFactory.getLogger(PerfilService.class);

    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final IAuthService authService;
    private final IAuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public PerfilResponse obtener(String username) {
        return aRespuesta(cuentaOFalla(username));
    }

    @Override
    @Transactional
    public PerfilResponse actualizar(String username, PerfilRequest r) {
        Cuenta cuenta = cuentaOFalla(username);
        Usuario usuario = cuenta.getUsuario();
        String email = r.getEmail().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailAndIdUsuarioNotAndEliminadoFalse(email, usuario.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otro usuario con ese correo");
        }
        usuario.setEmail(email);
        usuario.setTelefono(r.getTelefono() == null || r.getTelefono().isBlank() ? null : r.getTelefono().trim());
        usuarioRepository.save(usuario);
        return aRespuesta(cuenta);
    }

    @Override
    @Transactional
    public LoginResponse cambiarClave(String username, CambioClaveRequest r) {
        Cuenta cuenta = cuentaOFalla(username);
        // 400 y no 401: un 401 haría que el frontend cierre la sesión como si el token hubiera vencido
        if (!passwordEncoder.matches(r.getClaveActual(), cuenta.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
        }
        if (r.getClaveNueva().equals(r.getClaveActual())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta de la actual");
        }

        cuenta.setPasswordHash(passwordEncoder.encode(r.getClaveNueva()));
        cuenta.setDebeCambiarClave(false);
        cuenta.setIntentosFallidos(0);
        cuenta.setBloqueadoHasta(null);
        // Cierra las sesiones abiertas en otros lugares; la nueva sesión que se devuelve sí es válida
        cuenta.setTokenDesde(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        cuentaRepository.save(cuenta);

        auditar(cuenta, "CLAVE_CAMBIADA", "El usuario '" + cuenta.getUsername() + "' cambió su contraseña");
        return authService.emitirSesion(cuenta);
    }

    private Cuenta cuentaOFalla(String username) {
        return cuentaRepository.buscarPorUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
    }

    private void auditar(Cuenta cuenta, String accion, String detalle) {
        try {
            auditoriaService.registrar(cuenta.getUsuario().getIdUsuario(), accion, "cuenta", cuenta.getIdCuenta(), detalle);
        } catch (Exception e) {
            log.warn("No se pudo registrar la auditoría ({}): {}", accion, e.getMessage());
        }
    }

    private PerfilResponse aRespuesta(Cuenta c) {
        Usuario u = c.getUsuario();
        return PerfilResponse.builder()
                .idUsuario(u.getIdUsuario())
                .username(c.getUsername())
                .rol(c.getRol().getNombre())
                .documento(u.getDocumento())
                .nombres(u.getNombres())
                .apellidos(u.getApellidos())
                .email(u.getEmail())
                .telefono(u.getTelefono())
                .debeCambiarClave(Boolean.TRUE.equals(c.getDebeCambiarClave()))
                .build();
    }
}
