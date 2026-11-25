package com.biblosmast.service;

import com.biblosmast.model.dto.LoginResponse;
import com.biblosmast.model.dto.RegistroRequest;
import com.biblosmast.model.entity.Cuenta;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.model.entity.Rol;
import com.biblosmast.model.entity.Usuario;
import com.biblosmast.repository.CuentaRepository;
import com.biblosmast.repository.RolRepository;
import com.biblosmast.repository.UsuarioRepository;
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
public class RegistroService implements IRegistroService {

    private static final Logger log = LoggerFactory.getLogger(RegistroService.class);
    private static final String ROL_USUARIO = "USUARIO";

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final IAuthService authService;
    private final IAuditoriaService auditoriaService;

    @Override
    @Transactional
    public LoginResponse registrar(RegistroRequest r) {
        String documento = r.getDocumento().trim();
        String email = r.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = r.getUsername().trim();

        // Solo cuentas vigentes: si alguien fue dado de baja, su documento y correo quedan libres para registrarse de nuevo
        if (usuarioRepository.existsByDocumentoAndEliminadoFalse(documento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese documento");
        }
        if (usuarioRepository.existsByEmailAndEliminadoFalse(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
        }
        if (cuentaRepository.existsByUsernameAndEliminadaFalse(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese nombre de usuario ya está en uso");
        }
        Rol rol = rolRepository.findByNombre(ROL_USUARIO)
                .orElseThrow(() -> new IllegalStateException("Falta el rol USUARIO en la base de datos"));

        Usuario usuario = new Usuario();
        usuario.setDocumento(documento);
        usuario.setNombres(r.getNombres().trim());
        usuario.setApellidos(r.getApellidos().trim());
        usuario.setEmail(email);
        usuario.setTelefono(r.getTelefono() == null || r.getTelefono().isBlank() ? null : r.getTelefono().trim());
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);

        Cuenta cuenta = new Cuenta();
        cuenta.setUsuario(usuario);
        cuenta.setUsername(username);
        cuenta.setPasswordHash(passwordEncoder.encode(r.getPassword()));
        cuenta.setRol(rol);
        cuenta.setActivo(true);
        cuenta = cuentaRepository.save(cuenta);

        try {
            auditoriaService.registrar(usuario.getIdUsuario(), "REGISTRO", "usuario", usuario.getIdUsuario(),
                    "Registro propio de la cuenta '" + username + "'");
        } catch (Exception e) {
            log.warn("No se pudo registrar la auditoría del registro: {}", e.getMessage());
        }
        return authService.emitirSesion(cuenta);
    }
}
