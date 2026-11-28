package com.biblosmast.controller;

import com.biblosmast.model.dto.LoginRequest;
import com.biblosmast.model.dto.LoginResponse;
import com.biblosmast.model.dto.MensajeResponse;
import com.biblosmast.model.dto.RecuperarRequest;
import com.biblosmast.model.dto.RegistroRequest;
import com.biblosmast.model.dto.RestablecerRequest;
import com.biblosmast.security.LimitadorSolicitudes;
import com.biblosmast.service.IAuthService;
import com.biblosmast.service.IRecuperacionService;
import com.biblosmast.service.IRegistroService;
import java.security.Principal;
import java.time.Duration;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final IRegistroService registroService;
    private final IRecuperacionService recuperacionService;
    private final LimitadorSolicitudes limitador;

    /** El campo "username" acepta el nombre de usuario o el correo. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Cierra la sesión del usuario del token: ese token y los anteriores dejan de servir. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Principal principal) {
        authService.logout(principal.getName());
    }

    /** Pública: cualquiera puede crear su cuenta (rol USUARIO). Máximo 5 registros por hora desde una misma dirección. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse registro(@Valid @RequestBody RegistroRequest request, HttpServletRequest http) {
        limitar("registro:" + http.getRemoteAddr(), 5);
        return registroService.registrar(request);
    }

    /** Pública: siempre responde lo mismo, exista o no el correo. */
    @PostMapping("/recuperar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MensajeResponse recuperar(@Valid @RequestBody RecuperarRequest request, HttpServletRequest http) {
        limitar("recuperar:" + http.getRemoteAddr(), 10);
        recuperacionService.solicitar(request.getCorreo());
        return new MensajeResponse("Si el correo está registrado, te enviamos un enlace para cambiar tu contraseña. Revisa tu bandeja de entrada.");
    }

    /** Pública: usa el código del enlace recibido por correo. */
    @PostMapping("/restablecer")
    public MensajeResponse restablecer(@Valid @RequestBody RestablecerRequest request, HttpServletRequest http) {
        limitar("restablecer:" + http.getRemoteAddr(), 20);
        recuperacionService.restablecer(request.getToken(), request.getClaveNueva());
        return new MensajeResponse("Contraseña cambiada. Ya puedes iniciar sesión.");
    }

    private void limitar(String clave, int maximoPorHora) {
        if (!limitador.permitir(clave, maximoPorHora, Duration.ofHours(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes. Intenta de nuevo más tarde");
        }
    }
}
