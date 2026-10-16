package com.biblosmast.repository;

import com.biblosmast.model.entity.Ejemplar;
import com.biblosmast.model.entity.EstadoEjemplar;
import java.util.Collection;
import java.util.List;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EjemplarRepository extends JpaRepository<Ejemplar, Integer> {

    @EntityGraph(attributePaths = "libro")
    List<Ejemplar> findByEstado(EstadoEjemplar estado);

    @Override
    @EntityGraph(attributePaths = "libro")
    List<Ejemplar> findAll();

    @EntityGraph(attributePaths = "libro")
    List<Ejemplar> findByLibroIdLibro(Integer idLibro);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdEjemplarNot(String codigo, Integer idEjemplar);

    /** Bloquea las filas para que dos préstamos simultáneos no tomen el mismo ejemplar. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Ejemplar e join fetch e.libro where e.idEjemplar in :ids")
    List<Ejemplar> bloquearPorIds(@Param("ids") Collection<Integer> ids);

    /** Por libro: [idLibro, total de ejemplares, ejemplares disponibles]. */
    @Query("select e.libro.idLibro, count(e), "
            + "sum(case when e.estado = com.biblosmast.model.entity.EstadoEjemplar.DISPONIBLE then 1 else 0 end) "
            + "from Ejemplar e group by e.libro.idLibro")
    List<Object[]> contarPorLibro();
}
