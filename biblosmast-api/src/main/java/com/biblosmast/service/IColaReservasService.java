package com.biblosmast.service;

import com.biblosmast.model.entity.EstadoReserva;
import com.biblosmast.model.entity.Reserva;
import java.util.Collection;
import java.util.List;

/** Cola de reservas: aparta ejemplares devueltos para quien espera y libera los que no se recogieron. */
public interface IColaReservasService {

    /**
     * Si el libro tiene ejemplares DISPONIBLES y gente esperando, aparta un ejemplar por persona (en orden de llegada)
     * y deja esas reservas en LISTA. Devuelve las reservas que quedaron listas.
     */
    List<Reserva> asignarEjemplarLibre(Integer idLibro);

    /** Libera los ejemplares de las reservas LISTA que no se recogieron a tiempo y se los pasa al siguiente de la cola. */
    void vencerListas();

    /**
     * Cierra una reserva (CANCELADA o VENCIDA). Si estaba LISTA, libera el ejemplar apartado
     * y lo pasa al siguiente de la cola.
     */
    void cerrar(Reserva reserva, EstadoReserva nuevoEstado);

    /**
     * Marca la reserva como ATENDIDA porque el usuario se llevó esos ejemplares. Si estaba LISTA y se llevó otro ejemplar
     * distinto al apartado, el apartado se libera y pasa al siguiente de la cola.
     */
    void atender(Reserva reserva, Collection<Integer> idsEjemplaresEntregados);
}
