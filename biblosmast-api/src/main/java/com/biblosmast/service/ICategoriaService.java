package com.biblosmast.service;

import com.biblosmast.model.dto.CategoriaRequest;
import com.biblosmast.model.dto.CategoriaResponse;
import java.util.List;

public interface ICategoriaService {
    List<CategoriaResponse> listar();
    CategoriaResponse obtener(Integer id);
    CategoriaResponse crear(CategoriaRequest request);
    CategoriaResponse actualizar(Integer id, CategoriaRequest request);
    void eliminar(Integer id);
}
