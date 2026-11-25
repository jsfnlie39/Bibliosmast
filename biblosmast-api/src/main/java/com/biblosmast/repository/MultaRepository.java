package com.biblosmast.repository;

import com.biblosmast.model.entity.EstadoMulta;
import com.biblosmast.model.entity.Multa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MultaRepository extends JpaRepository<Multa, Integer> {

    @Query("select m from Multa m join fetch m.usuario order by m.fecha desc")
    List<Multa> listarTodas();

    @Query("select m from Multa m join fetch m.usuario where m.idMulta = :id")
    Optional<Multa> buscarConDetalle(@Param("id") Integer id);

    boolean existsByUsuarioIdUsuarioAndEstado(Integer idUsuario, EstadoMulta estado);
}
