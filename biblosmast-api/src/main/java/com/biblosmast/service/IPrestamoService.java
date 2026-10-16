package com.biblosmast.service;

import com.biblosmast.model.dto.DevolucionRequest;
import com.biblosmast.model.dto.PrestamoRequest;
import com.biblosmast.model.dto.PrestamoResponse;
import java.util.List;

public interface IPrestamoService {
    /** estado: ACTIVO, VENCIDO o FINALIZADO (null o vacío = todos). idUsuario opcional: historial de ese usuario. */
    List<PrestamoResponse> listar(String estado, Integer idUsuario);
    PrestamoResponse obtener(Integer id);
    PrestamoResponse crear(PrestamoRequest request);
    PrestamoResponse devolver(Integer idPrestamo, Integer idDetalle, DevolucionRequest request);
}
