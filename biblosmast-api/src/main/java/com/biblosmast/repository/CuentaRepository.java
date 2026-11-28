package com.biblosmast.repository;

import com.biblosmast.model.entity.Cuenta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    /** Cuenta vigente (no dada de baja) con ese nombre de usuario. */
    @Query("select c from Cuenta c join fetch c.usuario join fetch c.rol where c.username = :username and c.eliminada = false")
    Optional<Cuenta> buscarPorUsername(@Param("username") String username);

    /** Para iniciar sesión: sirve el nombre de usuario o el correo (sin distinguir mayúsculas). Solo cuentas vigentes. */
    @Query("select c from Cuenta c join fetch c.usuario u join fetch c.rol "
            + "where c.eliminada = false and (lower(c.username) = lower(:identificador) or lower(u.email) = lower(:identificador))")
    Optional<Cuenta> buscarPorUsernameOCorreo(@Param("identificador") String identificador);

    @Query("select c from Cuenta c join fetch c.rol where c.usuario.idUsuario = :idUsuario and c.eliminada = false")
    Optional<Cuenta> buscarPorIdUsuario(@Param("idUsuario") Integer idUsuario);

    /** Todas las cuentas, también las de usuarios dados de baja (el filtro se hace por usuario). */
    @Query("select c from Cuenta c join fetch c.usuario join fetch c.rol")
    List<Cuenta> listarConRol();

    boolean existsByUsernameAndEliminadaFalse(String username);

    boolean existsByUsernameAndIdCuentaNotAndEliminadaFalse(String username, Integer idCuenta);

    @Query("select count(c) from Cuenta c where c.rol.nombre = 'ADMIN' and c.activo = true and c.eliminada = false")
    long contarAdministradoresActivos();
}
