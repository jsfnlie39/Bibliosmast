package com.biblosmast.service;

import com.biblosmast.model.dto.LibroRequest;
import com.biblosmast.model.dto.LibroResponse;
import java.util.List;

public interface ILibroService {
    List<LibroResponse> listar(String busqueda);
    LibroResponse obtener(Integer id);
    LibroResponse crear(LibroRequest request);
    LibroResponse actualizar(Integer id, LibroRequest request);
    void eliminar(Integer id);
}
