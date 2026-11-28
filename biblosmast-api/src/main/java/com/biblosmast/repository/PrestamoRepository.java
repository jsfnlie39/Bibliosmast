package com.biblosmast.repository;

import com.biblosmast.model.entity.Prestamo;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrestamoRepository extends JpaRepository<Prestamo, Integer> {

    @EntityGraph(attributePaths = {"usuario", "bibliotecario", "detalles"})
    @Query("select distinct p from Prestamo p order by p.fechaPrestamo desc")
    List<Prestamo> listarTodos();

    boolean existsByUsuarioIdUsuarioAndEstado(Integer idUsuario, com.biblosmast.model.entity.EstadoPrestamo estado);

    /** Ejemplares que la persona tiene prestados y todavía no ha devuelto (cuenta contra el máximo de su tipo). */
    @Query("select count(d) from DetallePrestamo d where d.prestamo.usuario.idUsuario = :idUsuario "
            + "and d.fechaDevolucion is null")
    long contarEjemplaresSinDevolver(@Param("idUsuario") Integer idUsuario);

    /** Préstamos activos con la fecha límite anterior a la indicada (vencidos si es hoy). */
    long countByEstadoAndFechaLimiteBefore(com.biblosmast.model.entity.EstadoPrestamo estado, java.time.LocalDate fecha);

    /** Préstamos activos que vencen exactamente en la fecha indicada. */
    long countByEstadoAndFechaLimite(com.biblosmast.model.entity.EstadoPrestamo estado, java.time.LocalDate fecha);

    /** Préstamos hechos desde un instante (para agrupar por mes). */
    List<Prestamo> findByFechaPrestamoGreaterThanEqual(java.time.LocalDateTime desde);

    /** Préstamos del estado cuya fecha límite cae entre dos fechas (ambas incluidas). */
    List<Prestamo> findByEstadoAndFechaLimiteBetween(com.biblosmast.model.entity.EstadoPrestamo estado,
                                                     java.time.LocalDate desde, java.time.LocalDate hasta);

    /** Préstamos del estado con la fecha límite anterior a la indicada. */
    List<Prestamo> findByEstadoAndFechaLimiteBefore(com.biblosmast.model.entity.EstadoPrestamo estado,
                                                    java.time.LocalDate fecha);
}
