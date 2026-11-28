package com.biblosmast.repository;

import com.biblosmast.model.entity.DetallePrestamo;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetallePrestamoRepository extends JpaRepository<DetallePrestamo, Integer> {

    /** Todos los préstamos de ejemplares de un libro, del más reciente al más antiguo. */
    @Query("select d from DetallePrestamo d join fetch d.prestamo p join fetch p.usuario join fetch d.ejemplar e "
            + "where e.libro.idLibro = :idLibro order by p.fechaPrestamo desc, d.idDetalle desc")
    List<DetallePrestamo> historialDeLibro(@Param("idLibro") Integer idLibro);

    /** [idLibro, título, veces prestado] de los préstamos entre dos instantes, del más prestado al menos. */
    @Query("select e.libro.idLibro, e.libro.titulo, count(d) from DetallePrestamo d join d.ejemplar e join d.prestamo p "
            + "where p.fechaPrestamo >= :desde and p.fechaPrestamo < :hasta "
            + "group by e.libro.idLibro, e.libro.titulo order by count(d) desc, e.libro.titulo asc")
    List<Object[]> masPrestados(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, Pageable pageable);

    /** [idUsuario, nombres, apellidos, documento, ejemplares] que cada persona tiene sin devolver y vencidos a esa fecha. */
    @Query("select u.idUsuario, u.nombres, u.apellidos, u.documento, count(d) from DetallePrestamo d "
            + "join d.prestamo p join p.usuario u where d.fechaDevolucion is null and p.fechaLimite < :hoy "
            + "group by u.idUsuario, u.nombres, u.apellidos, u.documento")
    List<Object[]> vencidosPorUsuario(@Param("hoy") java.time.LocalDate hoy);
}
