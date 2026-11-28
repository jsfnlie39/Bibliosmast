package com.biblosmast.model.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aviso dentro del sistema para una persona (la campana de la barra superior). */
@Entity
@Table(name = "notificacion")
@Getter @Setter @NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoNotificacion tipo;

    @Column(name = "mensaje", nullable = false, length = 300)
    private String mensaje;

    /** Referencias sueltas (sin clave foránea): borrar una reserva no debe bloquearse por su aviso. */
    @Column(name = "id_prestamo")
    private Integer idPrestamo;

    @Column(name = "id_reserva")
    private Integer idReserva;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "leida", nullable = false)
    private Boolean leida = false;

    /** Evita avisar dos veces lo mismo (por ejemplo POR_VENCER:12:2026-12-01). */
    @Column(name = "clave_unica", nullable = false, unique = true, length = 100)
    private String claveUnica;
}
