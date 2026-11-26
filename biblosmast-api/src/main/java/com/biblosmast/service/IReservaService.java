package com.biblosmast.service;

import com.biblosmast.model.dto.ReservaRequest;
import com.biblosmast.model.dto.ReservaResponse;
import java.util.List;

public interface IReservaService {
    /** estado e idUsuario opcionales (estado: PENDIENTE, LISTA, VENCIDA, ATENDIDA o CANCELADA). */
    List<ReservaResponse> listar(String estado, Integer idUsuario);
    ReservaResponse obtener(Integer id);
    ReservaResponse crear(ReservaRequest request);
    ReservaResponse actualizar(Integer id, ReservaRequest request);
    /** Cancela una reserva PENDIENTE o LISTA (si estaba lista, el ejemplar apartado pasa a la siguiente persona de la cola). */
    ReservaResponse cancelar(Integer id);
    void eliminar(Integer id);
}
