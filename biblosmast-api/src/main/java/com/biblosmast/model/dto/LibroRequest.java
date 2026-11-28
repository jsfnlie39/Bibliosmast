package com.biblosmast.model.dto;

import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LibroRequest {

    @NotBlank(message = "El ISBN es obligatorio")
    @Size(max = 20, message = "El ISBN no puede superar 20 caracteres")
    private String isbn;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede superar 200 caracteres")
    private String titulo;

    @NotNull(message = "La categoría es obligatoria")
    private Integer idCategoria;

    @NotNull(message = "La editorial es obligatoria")
    private Integer idEditorial;

    @Min(value = 1000, message = "Año inválido")
    @Max(value = 2100, message = "Año inválido")
    private Integer anioPublicacion;

    @Size(max = 30, message = "La edición no puede superar 30 caracteres")
    private String edicion;

    @Size(max = 30, message = "El idioma no puede superar 30 caracteres")
    private String idioma;

    @Min(value = 1, message = "El número de páginas debe ser mayor a 0")
    private Integer numeroPaginas;

    private String descripcion;

    @NotEmpty(message = "Selecciona al menos un autor")
    private List<Integer> idsAutores;
}
