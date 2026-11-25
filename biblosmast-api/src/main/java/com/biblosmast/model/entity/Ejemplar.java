package com.biblosmast.model.entity;

import java.time.LocalDate;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ejemplar")
@Getter @Setter @NoArgsConstructor
public class Ejemplar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ejemplar")
    private Integer idEjemplar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_libro", nullable = false)
    private Libro libro;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "ubicacion", length = 60)
    private String ubicacion;

    // El estado lo cambian los triggers de la base de datos al prestar y devolver.
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoEjemplar estado = EstadoEjemplar.DISPONIBLE;

    @Column(name = "fecha_ingreso", insertable = false, updatable = false)
    private LocalDate fechaIngreso;
}
