package com.biblosmast.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reserva")
@Getter @Setter @NoArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Integer idReserva;

    /** Quien aparta el libro. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_libro", nullable = false)
    private Libro libro;

    @Column(name = "fecha_reserva", nullable = false, updatable = false)
    private LocalDateTime fechaReserva;

    /** Último día en que la reserva sigue vigente (inclusive). */
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    /** Cuando la reserva pasa a LISTA: último día para recoger el ejemplar apartado (inclusive). */
    @Column(name = "fecha_limite_recogida")
    private LocalDate fechaLimiteRecogida;

    /** Ejemplar apartado mientras la reserva está LISTA (o el que se entregó, si ya se atendió). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ejemplar")
    private Ejemplar ejemplar;

    // VENCIDA se guarda solo si una reserva LISTA no se recogió a tiempo; la PENDIENTE vencida: se calcula al consultar (PENDIENTE con fecha pasada), igual que en los préstamos.
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoReserva estado = EstadoReserva.PENDIENTE;
}
