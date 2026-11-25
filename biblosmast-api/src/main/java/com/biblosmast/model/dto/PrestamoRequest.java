package com.biblosmast.model.dto;

import java.time.LocalDate;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PrestamoRequest {

    @NotNull(message = "Selecciona el usuario que recibe el préstamo")
    private Integer idUsuario;

    @NotNull(message = "Falta el bibliotecario que registra el préstamo")
    private Integer idBibliotecario;

    @NotNull(message = "La fecha límite es obligatoria")
    private LocalDate fechaLimite;

    @NotEmpty(message = "Selecciona al menos un ejemplar")
    @Size(max = 5, message = "Un préstamo puede tener máximo 5 ejemplares")
    private List<Integer> idsEjemplares;

    @Size(max = 255, message = "Las observaciones no pueden superar 255 caracteres")
    private String observaciones;
}
