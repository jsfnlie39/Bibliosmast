package com.biblosmast.model.dto;

import lombok.Builder;
import lombok.Getter;

/** Búsqueda avanzada de libros: todos los campos son opcionales y se combinan con «y». */
@Getter @Builder
public class LibroFiltro {
    /** Texto en el título o el ISBN. */
    private String q;
    private Integer idAutor;
    private Integer idCategoria;
    private Integer idEditorial;
    /** Idioma exacto, sin distinguir mayúsculas. */
    private String idioma;
    private Integer anioDesde;
    private Integer anioHasta;
    /** true = solo libros con al menos un ejemplar disponible. */
    private boolean soloDisponibles;
}
