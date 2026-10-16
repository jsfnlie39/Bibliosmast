package com.biblosmast.repository;

import com.biblosmast.model.entity.Cuenta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    @Query("select c from Cuenta c join fetch c.usuario join fetch c.rol where c.username = :username")
    Optional<Cuenta> buscarPorUsername(@Param("username") String username);

    @Query("select c from Cuenta c join fetch c.rol where c.usuario.idUsuario = :idUsuario")
    Optional<Cuenta> buscarPorIdUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("select c from Cuenta c join fetch c.usuario join fetch c.rol")
    List<Cuenta> listarConRol();

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdCuentaNot(String username, Integer idCuenta);

    @Query("select count(c) from Cuenta c where c.rol.nombre = 'ADMIN' and c.activo = true")
    long contarAdministradoresActivos();
}
