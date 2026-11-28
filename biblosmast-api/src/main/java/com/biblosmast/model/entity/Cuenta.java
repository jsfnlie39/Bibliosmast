package com.biblosmast.model.entity;

import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cuenta")
@Getter @Setter @NoArgsConstructor
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta")
    private Integer idCuenta;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "username", nullable = false, unique = true, length = 40)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Claves incorrectas seguidas. Se reinicia al iniciar sesión bien o al bloquear la cuenta. */
    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    /** Mientras no llegue esta fecha, la cuenta no puede iniciar sesión. */
    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    /** True cuando tiene una clave temporal: debe cambiarla antes de usar el resto del sistema. */
    @Column(name = "debe_cambiar_clave", nullable = false)
    private Boolean debeCambiarClave = false;

    /** True cuando el usuario fue dado de baja: la cuenta se conserva (historial) pero ya no puede entrar. */
    @Column(name = "eliminada", nullable = false)
    private Boolean eliminada = false;

    /** Los tokens emitidos antes de esta fecha ya no valen (cerrar sesión, cambio o restablecimiento de clave). */
    @Column(name = "token_desde")
    private LocalDateTime tokenDesde;
}
