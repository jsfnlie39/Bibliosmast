package com.biblosmast.service;

import com.biblosmast.model.dto.FichaLibroResponse;
import com.biblosmast.model.dto.FichaUsuarioResponse;

/** Fichas con historial. Solo para el personal: incluyen datos de otras personas. */
public interface IFichaService {

    FichaLibroResponse libro(Integer idLibro);

    FichaUsuarioResponse usuario(Integer idUsuario);
}
