package com.biblosmast.service;

import com.biblosmast.model.dto.DevolucionRequest;
import com.biblosmast.model.dto.PrestamoRequest;
import com.biblosmast.model.dto.PrestamoResponse;
import java.util.List;

public interface IPrestamoService {
    /** estado: ACTIVO, VENCIDO o FINALIZADO (null o vacío = todos). idUsuario opcional: historial de ese usuario. */
    List<PrestamoResponse> listar(String estado, Integer idUsuario);
    PrestamoResponse obtener(Integer id);
    /** El bibliotecario que registra el préstamo es la persona con sesión (username del token), nunca un dato del cuerpo. */
    PrestamoResponse crear(PrestamoRequest request, String usernameBibliotecario);
    /** Extiende la fecha límite (máx. `max_renovaciones` veces). 409 si el préstamo no se puede renovar. */
    PrestamoResponse renovar(Integer idPrestamo);
    PrestamoResponse devolver(Integer idPrestamo, Integer idDetalle, DevolucionRequest request);
}
