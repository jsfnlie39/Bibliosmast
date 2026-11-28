package com.biblosmast.controller;

import com.biblosmast.model.dto.TipoUsuarioRequest;
import com.biblosmast.model.dto.TipoUsuarioResponse;
import com.biblosmast.service.ITipoUsuarioService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tipos de usuario y sus reglas de préstamo. Solo ADMIN (ver SeguridadWebConfig). */
@RestController
@RequestMapping("/api/tipos-usuario")
@RequiredArgsConstructor
public class TipoUsuarioController {

    private final ITipoUsuarioService tipoService;

    @GetMapping
    public List<TipoUsuarioResponse> listar() {
        return tipoService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TipoUsuarioResponse crear(@Valid @RequestBody TipoUsuarioRequest request) {
        return tipoService.crear(request);
    }

    @PutMapping("/{id}")
    public TipoUsuarioResponse actualizar(@PathVariable Integer id, @Valid @RequestBody TipoUsuarioRequest request) {
        return tipoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        tipoService.eliminar(id);
    }
}
