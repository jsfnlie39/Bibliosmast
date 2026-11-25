package com.biblosmast.controller;

import com.biblosmast.model.dto.CambioClaveRequest;
import com.biblosmast.model.dto.LoginResponse;
import com.biblosmast.model.dto.PerfilRequest;
import com.biblosmast.model.dto.PerfilResponse;
import com.biblosmast.service.IPerfilService;
import java.security.Principal;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** «Mi perfil»: la persona sale siempre del token. */
@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final IPerfilService perfilService;

    @GetMapping
    public PerfilResponse obtener(Principal principal) {
        return perfilService.obtener(principal.getName());
    }

    @PutMapping
    public PerfilResponse actualizar(Principal principal, @Valid @RequestBody PerfilRequest request) {
        return perfilService.actualizar(principal.getName(), request);
    }

    @PutMapping("/clave")
    public LoginResponse cambiarClave(Principal principal, @Valid @RequestBody CambioClaveRequest request) {
        return perfilService.cambiarClave(principal.getName(), request);
    }
}
