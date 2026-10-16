package com.biblosmast.repository;

import com.biblosmast.model.entity.Reserva;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    @Query("select r from Reserva r join fetch r.usuario join fetch r.libro order by r.fechaReserva desc")
    List<Reserva> listarTodas();

    @Query("select r from Reserva r join fetch r.usuario join fetch r.libro where r.idReserva = :id")
    Optional<Reserva> buscarConDetalle(@Param("id") Integer id);

    /** Reservas pendientes y vigentes de un usuario para cualquiera de esos libros (las que un préstamo atiende). */
    @Query("select r from Reserva r where r.usuario.idUsuario = :idUsuario and r.libro.idLibro in :idsLibros "
            + "and r.estado = com.biblosmast.model.entity.EstadoReserva.PENDIENTE and r.fechaVencimiento >= :hoy")
    List<Reserva> pendientesVigentes(@Param("idUsuario") Integer idUsuario,
                                     @Param("idsLibros") Collection<Integer> idsLibros,
                                     @Param("hoy") LocalDate hoy);

    /** Reservas pendientes y todavía vigentes del mismo usuario y libro (se excluye una por id al editar). */
    @Query("select count(r) from Reserva r where r.usuario.idUsuario = :idUsuario and r.libro.idLibro = :idLibro "
            + "and r.estado = com.biblosmast.model.entity.EstadoReserva.PENDIENTE "
            + "and r.fechaVencimiento >= :hoy and r.idReserva <> :excluirId")
    long contarVigentes(@Param("idUsuario") Integer idUsuario, @Param("idLibro") Integer idLibro,
                        @Param("hoy") LocalDate hoy, @Param("excluirId") Integer excluirId);
}
