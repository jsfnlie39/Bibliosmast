package com.biblosmast.model.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TipoUsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 30, message = "El nombre no puede superar 30 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 _-]+$", message = "El nombre solo admite letras, números, espacios, guion y guion bajo")
    private String nombre;

    @NotNull(message = "El máximo de libros es obligatorio")
    @Min(value = 1, message = "El máximo de libros debe ser al menos 1")
    @Max(value = 20, message = "El máximo de libros no puede superar 20")
    private Integer maxLibros;

    @NotNull(message = "Los días de préstamo son obligatorios")
    @Min(value = 1, message = "Los días de préstamo deben ser al menos 1")
    @Max(value = 90, message = "Los días de préstamo no pueden superar 90")
    private Integer diasPrestamo;

    @NotNull(message = "El máximo de renovaciones es obligatorio")
    @Min(value = 0, message = "El máximo de renovaciones no puede ser negativo")
    @Max(value = 10, message = "El máximo de renovaciones no puede superar 10")
    private Integer maxRenovaciones;
}
