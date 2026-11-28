package com.biblosmast.model.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** Resultado de una pasada de las tareas diarias. Los avisos creados salen de los conteos de préstamos por vencer y vencidos. */
@Getter @Builder
public class ResumenTareasResponse {
    private LocalDateTime ejecutadoEn;
    /** Reservas LISTA que no se recogieron a tiempo: pasaron a VENCIDA y su ejemplar siguió al próximo de la cola. */
    private int reservasListasVencidas;
    /** Préstamos activos con la fecha límite pasada (se muestran como VENCIDO; el retraso se cobra al devolver). */
    private long prestamosVencidos;
    /** Préstamos activos que vencen mañana. */
    private long prestamosQueVencenManana;
    /** Reservas LISTA cuyo último día para recoger es hoy. */
    private long reservasListasQueVencenHoy;
    /** Avisos nuevos creados en esta pasada (los que ya existían no se repiten). */
    private int avisosCreados;
}
