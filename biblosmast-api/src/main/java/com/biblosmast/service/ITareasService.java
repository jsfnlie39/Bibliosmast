package com.biblosmast.service;

import com.biblosmast.model.dto.ResumenTareasResponse;

/** Tareas de mantenimiento diario. Las mismas que corre el programador, para poder lanzarlas a mano. */
public interface ITareasService {

    /**
     * 1) Vence las reservas LISTA que no se recogieron y pasa el ejemplar al siguiente de la cola.
     * 2) Cuenta lo que vence pronto (préstamos y recogidas).
     * 3) Crea los avisos de «por vencer» y «vencido» (dentro del sistema y por correo), una sola vez por préstamo y fecha.
     * No cobra multas: el retraso se cobra solo al devolver el ejemplar, así que nunca se cobra dos veces.
     * Se puede ejecutar varias veces el mismo día sin efectos repetidos.
     */
    ResumenTareasResponse ejecutar();
}
