package com.biblosmast.model.dto;

import com.biblosmast.model.entity.EstadoEjemplar;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EjemplarRequest {

    @NotNull(message = "Selecciona el libro")
    private Integer idLibro;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 30, message = "El código no puede superar 30 caracteres")
    private String codigo;

    @Size(max = 60, message = "La ubicación no puede superar 60 caracteres")
    private String ubicacion;

    /** Opcional al crear (queda DISPONIBLE). PRESTADO no se asigna a mano: lo hacen los préstamos. */
    private EstadoEjemplar estado;
}
