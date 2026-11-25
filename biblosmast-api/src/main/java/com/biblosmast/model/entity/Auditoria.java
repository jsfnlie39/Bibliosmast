package com.biblosmast.model.entity;

import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auditoria")
@Getter @Setter @NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer idAuditoria;

    /** Quien hizo la acción (null si no se sabe o el usuario ya se eliminó). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    /** LOGIN, LOGIN_FALLIDO, CREAR, ACTUALIZAR, ELIMINAR, DEVOLUCION o PAGO. */
    @Column(name = "accion", nullable = false, length = 30)
    private String accion;

    @Column(name = "tabla", length = 50)
    private String tabla;

    @Column(name = "id_registro")
    private Integer idRegistro;

    @Column(name = "detalle", length = 255)
    private String detalle;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;
}
