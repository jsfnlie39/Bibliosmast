package com.biblosmast.repository;

import com.biblosmast.model.entity.Notificacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    boolean existsByClaveUnica(String claveUnica);

    List<Notificacion> findTop30ByUsuarioIdUsuarioOrderByFechaCreacionDescIdNotificacionDesc(Integer idUsuario);

    long countByUsuarioIdUsuarioAndLeidaFalse(Integer idUsuario);

    Optional<Notificacion> findByIdNotificacionAndUsuarioIdUsuario(Integer idNotificacion, Integer idUsuario);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Notificacion n set n.leida = true where n.usuario.idUsuario = :idUsuario and n.leida = false")
    int marcarTodasLeidas(@Param("idUsuario") Integer idUsuario);
}
