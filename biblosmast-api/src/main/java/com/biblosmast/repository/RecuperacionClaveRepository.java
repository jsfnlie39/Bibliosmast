package com.biblosmast.repository;

import com.biblosmast.model.entity.RecuperacionClave;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecuperacionClaveRepository extends JpaRepository<RecuperacionClave, Integer> {

    @EntityGraph(attributePaths = {"cuenta", "cuenta.usuario"})
    Optional<RecuperacionClave> findByTokenHash(String tokenHash);

    List<RecuperacionClave> findByCuentaIdCuentaAndUsadaEnIsNull(Integer idCuenta);

    boolean existsByCuentaIdCuentaAndCreadaEnAfter(Integer idCuenta, LocalDateTime desde);
}
