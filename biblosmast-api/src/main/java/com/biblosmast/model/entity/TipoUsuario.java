package com.biblosmast.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Reglas de préstamo según quién es la persona (ESTUDIANTE, DOCENTE, EXTERNO...). */
@Entity
@Table(name = "tipo_usuario")
@Getter @Setter @NoArgsConstructor
public class TipoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_usuario")
    private Integer idTipoUsuario;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    /** Máximo de libros sin devolver al mismo tiempo. */
    @Column(name = "max_libros", nullable = false)
    private Integer maxLibros;

    /** Días de un préstamo nuevo y días que suma cada renovación. */
    @Column(name = "dias_prestamo", nullable = false)
    private Integer diasPrestamo;

    @Column(name = "max_renovaciones", nullable = false)
    private Integer maxRenovaciones;
}
