package com.biblosmast.config;

import com.biblosmast.service.ITareasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Programa las tareas diarias. Se apaga con `biblosmast.tareas.habilitadas=false` (por ejemplo, para correr
 * varias copias de la API sin que todas ejecuten la tarea). La hora se cambia con `biblosmast.tareas.cron`.
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "biblosmast.tareas.habilitadas", havingValue = "true", matchIfMissing = true)
public class TareasProgramadas {

    private final ITareasService tareasService;

    @Scheduled(cron = "${biblosmast.tareas.cron:0 0 2 * * *}", zone = "${biblosmast.tareas.zona:America/Bogota}")
    public void ejecutarDiarias() {
        try {
            tareasService.ejecutar();
        } catch (Exception e) {
            // Una falla no debe detener el programador: se reintenta en la próxima pasada
            log.error("Fallaron las tareas diarias: {}", e.getMessage(), e);
        }
    }
}
