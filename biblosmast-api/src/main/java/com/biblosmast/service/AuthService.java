package com.biblosmast.service;

import com.biblosmast.model.dto.LoginRequest;
import com.biblosmast.model.dto.LoginResponse;
import com.biblosmast.model.entity.Cuenta;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private static final String MENSAJE_INVALIDO = "Usuario o contraseña incorrectos";
    // Hash BCrypt de relleno: se compara igual cuando el usuario no existe,
    // para que el tiempo de respuesta no revele si el usuario existe o no.
    private static final String HASH_RELLENO = "$2a$10$nIrHXnZljgQngFyUUhpJpekfHxrNjBBOMiASF.2GKC0l/CTBeyyni";

    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Cuenta cuenta = cuentaRepository.buscarPorUsername(request.getUsername().trim()).orElse(null);

        String hash = (cuenta != null) ? cuenta.getPasswordHash() : HASH_RELLENO;
        boolean passwordOk = passwordEncoder.matches(request.getPassword(), hash);

        if (cuenta == null || !passwordOk) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, MENSAJE_INVALIDO);
        }
        if (!Boolean.TRUE.equals(cuenta.getActivo())
                || cuenta.getUsuario().getEstado() == EstadoUsuario.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta está inactiva");
        }

        return new LoginResponse(
                cuenta.getUsuario().getIdUsuario(),
                cuenta.getUsername(),
                cuenta.getUsuario().getNombres(),
                cuenta.getUsuario().getApellidos(),
                cuenta.getRol().getNombre());
    }
}
