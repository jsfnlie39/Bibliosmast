package com.biblosmast.service;

import com.biblosmast.model.dto.ClaveTemporalResponse;
import com.biblosmast.model.dto.UsuarioRequest;
import com.biblosmast.model.dto.UsuarioResponse;
import java.util.List;

public interface IUsuarioService {
    /** soloDadosDeBaja: false = usuarios vigentes; true = los que fueron dados de baja (consulta de historial). */
    List<UsuarioResponse> listar(String busqueda, boolean soloDadosDeBaja);
    UsuarioResponse obtener(Integer id);
    UsuarioResponse crear(UsuarioRequest request);
    UsuarioResponse actualizar(Integer id, UsuarioRequest request);
    void eliminar(Integer id);

    /** Pone una contraseña temporal (obliga a cambiarla), desbloquea la cuenta y cierra sus sesiones. */
    ClaveTemporalResponse restablecerClave(Integer id, String actor);
}
