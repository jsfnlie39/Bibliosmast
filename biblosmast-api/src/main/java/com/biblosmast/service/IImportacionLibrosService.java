package com.biblosmast.service;

import com.biblosmast.model.dto.ImportacionLibrosResponse;
import com.biblosmast.model.dto.ImportarLibrosRequest;

/** Importación de libros desde un CSV: se revisa primero y se guarda todo junto o nada. */
public interface IImportacionLibrosService {

    /**
     * Columnas obligatorias: isbn, titulo, autores (varios separados por |), categoria y editorial (por nombre).
     * Opcionales: anio, idioma, edicion, paginas, descripcion. Con {@code soloValidar} no guarda nada; si no, guarda
     * todos los libros en una sola transacción, y solo si ninguna línea tiene errores.
     */
    ImportacionLibrosResponse importar(ImportarLibrosRequest request, String username);
}
