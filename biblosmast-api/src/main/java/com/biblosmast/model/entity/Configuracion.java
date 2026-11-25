package com.biblosmast.model.entity;

import java.math.BigDecimal;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracion")
@Getter @Setter @NoArgsConstructor
public class Configuracion {

    @Id
    @Column(name = "clave", length = 50)
    private String clave;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "descripcion", nullable = false, length = 150)
    private String descripcion;
}
