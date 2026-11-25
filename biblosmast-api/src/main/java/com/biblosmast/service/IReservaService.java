package com.biblosmast.service;

import com.biblosmast.model.dto.ReservaRequest;
import com.biblosmast.model.dto.ReservaResponse;
import java.util.List;

public interface IReservaService {
    /** estado e idUsuario opcionales (estado: PENDIENTE, VENCIDA, ATENDIDA o CANCELADA). */
    List<ReservaResponse> listar(String estado, Integer idUsuario);
    ReservaResponse obtener(Integer id);
    ReservaResponse crear(ReservaRequest request);
    ReservaResponse actualizar(Integer id, ReservaRequest request);
    void eliminar(Integer id);
}
