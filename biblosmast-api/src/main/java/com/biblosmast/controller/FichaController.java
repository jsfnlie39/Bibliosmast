package com.biblosmast.controller;

import com.biblosmast.model.dto.FichaLibroResponse;
import com.biblosmast.model.dto.FichaUsuarioResponse;
import com.biblosmast.service.IFichaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fichas con historial de libros y usuarios. Solo ADMIN y BIBLIOTECARIO (ver SeguridadWebConfig). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FichaController {

    private final IFichaService fichaService;

    @GetMapping("/libros/{id}/historial")
    public FichaLibroResponse libro(@PathVariable Integer id) {
        return fichaService.libro(id);
    }

    @GetMapping("/usuarios/{id}/historial")
    public FichaUsuarioResponse usuario(@PathVariable Integer id) {
        return fichaService.usuario(id);
    }
}
