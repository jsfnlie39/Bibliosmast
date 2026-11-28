package com.biblosmast.service;

import com.biblosmast.model.dto.TipoUsuarioRequest;
import com.biblosmast.model.dto.TipoUsuarioResponse;
import java.util.List;

public interface ITipoUsuarioService {

    List<TipoUsuarioResponse> listar();

    TipoUsuarioResponse crear(TipoUsuarioRequest request);

    /** El tipo EXTERNO (el que recibe quien se registra solo) no se puede renombrar. */
    TipoUsuarioResponse actualizar(Integer id, TipoUsuarioRequest request);

    /** 409 si hay usuarios con ese tipo o si es EXTERNO. */
    void eliminar(Integer id);
}
