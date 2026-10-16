package com.biblosmast.service;

import com.biblosmast.model.dto.UsuarioRequest;
import com.biblosmast.model.dto.UsuarioResponse;
import java.util.List;

public interface IUsuarioService {
    List<UsuarioResponse> listar(String busqueda);
    UsuarioResponse obtener(Integer id);
    UsuarioResponse crear(UsuarioRequest request);
    UsuarioResponse actualizar(Integer id, UsuarioRequest request);
    void eliminar(Integer id);
}
