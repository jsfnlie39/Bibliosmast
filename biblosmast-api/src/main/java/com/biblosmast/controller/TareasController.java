package com.biblosmast.controller;

import com.biblosmast.model.dto.ResumenTareasResponse;
import com.biblosmast.service.ITareasService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lanzar las tareas diarias a mano. Solo ADMIN (ver SeguridadWebConfig). */
@RestController
@RequestMapping("/api/tareas")
@RequiredArgsConstructor
public class TareasController {

    private final ITareasService tareasService;

    @PostMapping("/ejecutar")
    public ResumenTareasResponse ejecutar() {
        return tareasService.ejecutar();
    }
}
