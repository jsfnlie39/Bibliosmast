package com.biblosmast.controller;

import com.biblosmast.model.dto.LibroPrestadoItem;
import com.biblosmast.model.dto.PrestamosMesItem;
import com.biblosmast.model.dto.UsuarioRetrasoItem;
import com.biblosmast.service.IReporteService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reportes. Solo ADMIN y BIBLIOTECARIO (ver SeguridadWebConfig). Fechas AAAA-MM-DD, opcionales. */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final IReporteService reporteService;

    @GetMapping("/libros-mas-prestados")
    public List<LibroPrestadoItem> librosMasPrestados(
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "limite", defaultValue = "10") int limite) {
        return reporteService.librosMasPrestados(desde, hasta, limite);
    }

    @GetMapping("/usuarios-con-retrasos")
    public List<UsuarioRetrasoItem> usuariosConRetrasos(
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "limite", defaultValue = "10") int limite) {
        return reporteService.usuariosConRetrasos(desde, hasta, limite);
    }

    @GetMapping("/prestamos-por-mes")
    public List<PrestamosMesItem> prestamosPorMes(@RequestParam(name = "meses", defaultValue = "12") int meses) {
        return reporteService.prestamosPorMes(meses);
    }
}
