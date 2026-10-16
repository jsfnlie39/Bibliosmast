package com.biblosmast.service;

import com.biblosmast.model.dto.EditorialRequest;
import com.biblosmast.model.dto.EditorialResponse;
import java.util.List;

public interface IEditorialService {
    List<EditorialResponse> listar();
    EditorialResponse obtener(Integer id);
    EditorialResponse crear(EditorialRequest request);
    EditorialResponse actualizar(Integer id, EditorialRequest request);
    void eliminar(Integer id);
}
