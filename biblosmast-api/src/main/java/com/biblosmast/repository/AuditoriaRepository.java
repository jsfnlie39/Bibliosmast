package com.biblosmast.repository;

import com.biblosmast.model.entity.Auditoria;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {

    /**
     * Filtros sin parámetros nulos: acción "%" = todas e idUsuario -1 = todos.
     * El rango de fechas es [desde, hasta).
     */
    @Query("select a from Auditoria a left join fetch a.usuario "
            + "where a.accion like :accion "
            + "and (:idUsuario = -1 or a.usuario.idUsuario = :idUsuario) "
            + "and a.fecha >= :desde and a.fecha < :hasta "
            + "order by a.fecha desc, a.idAuditoria desc")
    List<Auditoria> buscar(@Param("accion") String accion,
                           @Param("idUsuario") Integer idUsuario,
                           @Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta,
                           Pageable pageable);
}
