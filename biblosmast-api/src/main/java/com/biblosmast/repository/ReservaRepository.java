package com.biblosmast.repository;

import com.biblosmast.model.entity.EstadoReserva;
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

    /** Reservas del mismo usuario y libro que siguen abiertas: PENDIENTE vigente o LISTA (se excluye una por id al editar). */
    @Query("select count(r) from Reserva r where r.usuario.idUsuario = :idUsuario and r.libro.idLibro = :idLibro "
            + "and ((r.estado = com.biblosmast.model.entity.EstadoReserva.PENDIENTE and r.fechaVencimiento >= :hoy) "
            + "or r.estado = com.biblosmast.model.entity.EstadoReserva.LISTA) and r.idReserva <> :excluirId")
    long contarVigentes(@Param("idUsuario") Integer idUsuario, @Param("idLibro") Integer idLibro,
                        @Param("hoy") LocalDate hoy, @Param("excluirId") Integer excluirId);

    /** Reservas pendientes y vigentes de OTRAS personas para esos libros (impiden renovar un préstamo). */
    @Query("select count(r) from Reserva r where r.usuario.idUsuario <> :idUsuario and r.libro.idLibro in :idsLibros "
            + "and r.estado = com.biblosmast.model.entity.EstadoReserva.PENDIENTE and r.fechaVencimiento >= :hoy")
    long contarPendientesDeOtros(@Param("idUsuario") Integer idUsuario,
                                 @Param("idsLibros") Collection<Integer> idsLibros,
                                 @Param("hoy") LocalDate hoy);

    /** La cola de un libro: pendientes y vigentes, la más antigua primero. */
    @Query("select r from Reserva r join fetch r.usuario where r.libro.idLibro = :idLibro "
            + "and r.estado = com.biblosmast.model.entity.EstadoReserva.PENDIENTE and r.fechaVencimiento >= :hoy "
            + "order by r.fechaReserva asc, r.idReserva asc")
    List<Reserva> colaDeLibro(@Param("idLibro") Integer idLibro, @Param("hoy") LocalDate hoy);

    /** Reservas LISTA cuyo plazo de recogida ya pasó (hay que liberar el ejemplar apartado). */
    @Query("select r from Reserva r join fetch r.libro where r.estado = com.biblosmast.model.entity.EstadoReserva.LISTA "
            + "and r.fechaLimiteRecogida < :hoy")
    List<Reserva> listasVencidas(@Param("hoy") LocalDate hoy);

    /** Reservas LISTA de un usuario, con el ejemplar apartado y su libro (el préstamo las atiende). */
    @Query("select r from Reserva r join fetch r.ejemplar e join fetch e.libro where r.usuario.idUsuario = :idUsuario "
            + "and r.estado = com.biblosmast.model.entity.EstadoReserva.LISTA")
    List<Reserva> listasDeUsuario(@Param("idUsuario") Integer idUsuario);

    /** ¿Este ejemplar está apartado para alguien? */
    boolean existsByEjemplarIdEjemplarAndEstado(Integer idEjemplar, EstadoReserva estado);

    /** Reservas de un usuario en un estado (la baja del usuario cancela las pendientes). */
    List<Reserva> findByUsuarioIdUsuarioAndEstado(Integer idUsuario, EstadoReserva estado);
}
