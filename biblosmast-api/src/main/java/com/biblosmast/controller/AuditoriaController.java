package com.biblosmast.controller;

import com.biblosmast.model.dto.AuditoriaResponse;
import com.biblosmast.service.IAuditoriaService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final IAuditoriaService auditoriaService;

    @GetMapping
    public List<AuditoriaResponse> listar(
            @RequestParam(name = "accion", required = false) String accion,
            @RequestParam(name = "idUsuario", required = false) Integer idUsuario,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "limite", required = false) Integer limite) {
        return auditoriaService.listar(accion, idUsuario, desde, hasta, limite);
    }
}
