package com.biblosmast.service;

import com.biblosmast.model.dto.EjemplarRequest;
import com.biblosmast.model.dto.EjemplarResponse;
import java.util.List;

public interface IEjemplarService {
    /** idLibro opcional: si viene, solo los ejemplares de ese libro. */
    List<EjemplarResponse> listar(Integer idLibro);
    EjemplarResponse obtener(Integer id);
    EjemplarResponse crear(EjemplarRequest request);
    EjemplarResponse actualizar(Integer id, EjemplarRequest request);
    void eliminar(Integer id);
}
