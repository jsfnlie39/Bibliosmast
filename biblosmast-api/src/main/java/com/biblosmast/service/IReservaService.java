package com.biblosmast.service;

import com.biblosmast.model.dto.ReservaRequest;
import com.biblosmast.model.dto.ReservaResponse;
import java.util.List;

public interface IReservaService {
    /** estado e idUsuario opcionales (estado: PENDIENTE, LISTA, VENCIDA, ATENDIDA o CANCELADA). */
    /** La persona cancela una reserva suya (PENDIENTE o LISTA). Si la reserva es de otra persona responde 404. */
    ReservaResponse cancelarPropia(Integer idReserva, Integer idUsuario);

    /** Todas las reservas de un libro (cola y cerradas), para su ficha. */
    List<ReservaResponse> listarPorLibro(Integer idLibro);

    List<ReservaResponse> listar(String estado, Integer idUsuario);
    ReservaResponse obtener(Integer id);
    ReservaResponse crear(ReservaRequest request);
    ReservaResponse actualizar(Integer id, ReservaRequest request);
    void eliminar(Integer id);
}
