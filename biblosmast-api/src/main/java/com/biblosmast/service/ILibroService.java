package com.biblosmast.service;

import com.biblosmast.model.dto.LibroFiltro;
import com.biblosmast.model.dto.LibroRequest;
import com.biblosmast.model.dto.LibroResponse;
import java.util.List;

public interface ILibroService {
    List<LibroResponse> listar(String busqueda);
    /** Búsqueda avanzada: texto, autor, categoría, editorial, idioma, rango de años y solo disponibles. */
    List<LibroResponse> buscar(LibroFiltro filtro);
    LibroResponse obtener(Integer id);
    LibroResponse crear(LibroRequest request);
    LibroResponse actualizar(Integer id, LibroRequest request);
    void eliminar(Integer id);
}
