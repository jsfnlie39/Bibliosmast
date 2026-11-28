package com.biblosmast.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ImportarLibrosRequest {

    /** Texto completo del archivo CSV (UTF-8). */
    @NotBlank(message = "El archivo está vacío")
    @Size(max = 1_000_000, message = "El archivo es demasiado grande (máximo 1 MB)")
    private String contenido;

    /** true (por omisión) = solo revisar y mostrar la vista previa, sin guardar nada. */
    private boolean soloValidar = true;
}
