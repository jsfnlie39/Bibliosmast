package com.biblosmast.service;

import com.biblosmast.model.dto.AutorRequest;
import com.biblosmast.model.dto.AutorResponse;
import java.util.List;

public interface IAutorService {
    List<AutorResponse> listar();
    AutorResponse obtener(Integer id);
    AutorResponse crear(AutorRequest request);
    AutorResponse actualizar(Integer id, AutorRequest request);
    void eliminar(Integer id);
}
