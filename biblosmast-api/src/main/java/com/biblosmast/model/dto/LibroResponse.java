package com.biblosmast.model.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class LibroResponse {
    private Integer idLibro;
    private String isbn;
    private String titulo;
    private Integer idCategoria;
    private String categoria;
    private Integer idEditorial;
    private String editorial;
    private Integer anioPublicacion;
    private String edicion;
    private String idioma;
    private Integer numeroPaginas;
    private String descripcion;
    private String estado;
    private List<Integer> idsAutores;
    private List<String> autores;
    private long totalEjemplares;
    private long ejemplaresDisponibles;
}
