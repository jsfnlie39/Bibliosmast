package com.biblosmast.controller;

import com.biblosmast.model.dto.OpcionDto;
import com.biblosmast.service.CatalogoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/opciones/categorias")
    public List<OpcionDto> categorias() {
        return catalogoService.categorias();
    }

    @GetMapping("/opciones/editoriales")
    public List<OpcionDto> editoriales() {
        return catalogoService.editoriales();
    }

    @GetMapping("/opciones/autores")
    public List<OpcionDto> autores() {
        return catalogoService.autores();
    }

    @GetMapping("/usuarios/activos")
    public List<OpcionDto> usuariosActivos() {
        return catalogoService.usuariosActivos();
    }

    @GetMapping("/ejemplares/disponibles")
    public List<OpcionDto> ejemplaresDisponibles() {
        return catalogoService.ejemplaresDisponibles();
    }

    @GetMapping("/roles")
    public List<OpcionDto> roles() {
        return catalogoService.roles();
    }
}
