package com.biblosmast.service;

import com.biblosmast.model.dto.ReservaRequest;
import com.biblosmast.model.dto.ReservaResponse;
import com.biblosmast.model.entity.EstadoEjemplar;
import com.biblosmast.model.entity.EstadoLibro;
import com.biblosmast.model.entity.EstadoReserva;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.model.entity.Libro;
import com.biblosmast.model.entity.Reserva;
import com.biblosmast.model.entity.Usuario;
import com.biblosmast.repository.EjemplarRepository;
import com.biblosmast.repository.LibroRepository;
import com.biblosmast.repository.ReservaRepository;
import com.biblosmast.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReservaService implements IReservaService {

    /** Id que nunca existe: se usa al crear, cuando no hay ninguna reserva que excluir del chequeo de duplicados. */
    private static final Integer SIN_EXCLUIR = -1;

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;
    private final EjemplarRepository ejemplarRepository;
    private final IColaReservasService colaReservas;

    // No es de solo lectura: antes de listar se liberan los ejemplares apartados que nadie recogió a tiempo.
    @Override
    @Transactional
    public List<ReservaResponse> listar(String estado, Integer idUsuario) {
        colaReservas.vencerListas();
        String filtro = (estado == null) ? "" : estado.trim().toUpperCase();
        Map<Integer, Long> disponibles = disponiblesPorLibro();
        List<Reserva> todas = reservaRepository.listarTodas();
        Map<Integer, Integer> posiciones = posiciones(todas);
        return todas.stream()
                .map(r -> aRespuesta(r, disponibles, posiciones))
                .filter(r -> filtro.isEmpty() || r.getEstado().equals(filtro))
                .filter(r -> idUsuario == null || r.getIdUsuario().equals(idUsuario))
                .toList();
    }

    @Override
    @Transactional
    public ReservaResponse obtener(Integer id) {
        colaReservas.vencerListas();
        return respuesta(buscarOFalla(id));
    }

    @Override
    @Transactional
    public ReservaResponse crear(ReservaRequest r) {
        colaReservas.vencerListas();
        if (r.getEstado() != null && r.getEstado() != EstadoReserva.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Una reserva nueva siempre queda PENDIENTE");
        }
        Usuario usuario = usuarioOFalla(r.getIdUsuario());
        Libro libro = libroOFalla(r.getIdLibro());
        exigirUsuarioActivo(usuario);
        exigirLibroActivo(libro);
        if (!r.getFechaVencimiento().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de vencimiento debe ser posterior a hoy");
        }
        exigirLibroSinDisponibles(libro);
        exigirSinDuplicada(usuario.getIdUsuario(), libro.getIdLibro(), SIN_EXCLUIR);

        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setLibro(libro);
        reserva.setFechaReserva(LocalDateTime.now());
        reserva.setFechaVencimiento(r.getFechaVencimiento());
        reserva.setEstado(EstadoReserva.PENDIENTE);
        return respuesta(reservaRepository.save(reserva));
    }

    @Override
    @Transactional
    public ReservaResponse actualizar(Integer id, ReservaRequest r) {
        colaReservas.vencerListas();
        Reserva reserva = buscarOFalla(id);
        if (reserva.getEstado() == EstadoReserva.LISTA) {
            return actualizarLista(reserva, r);
        }
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Una reserva atendida, cancelada o que venció sin recogerse ya no se puede editar");
        }
        EstadoReserva nuevo = (r.getEstado() == null) ? EstadoReserva.PENDIENTE : r.getEstado();
        if (nuevo == EstadoReserva.VENCIDA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El estado VENCIDA se calcula solo con la fecha de vencimiento");
        }
        if (nuevo == EstadoReserva.LISTA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Una reserva pasa sola a LISTA cuando se devuelve un ejemplar y le toca el turno");
        }
        Usuario usuario = usuarioOFalla(r.getIdUsuario());
        Libro libro = libroOFalla(r.getIdLibro());

        // Solo una reserva que sigue pendiente debe cumplir las reglas de vigencia;
        // atender o cancelar siempre se puede, aunque el usuario o el libro ya no estén activos.
        if (nuevo == EstadoReserva.PENDIENTE) {
            exigirUsuarioActivo(usuario);
            exigirLibroActivo(libro);
            LocalDate hoy = LocalDate.now();
            LocalDate fecha = r.getFechaVencimiento();
            boolean sinCambio = fecha.equals(reserva.getFechaVencimiento());
            if (sinCambio && fecha.isBefore(hoy)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La reserva está vencida: elige una nueva fecha de vencimiento o cámbiala a Cancelada");
            }
            if (!sinCambio && !fecha.isAfter(hoy)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La fecha de vencimiento debe ser posterior a hoy");
            }
            if (!libro.getIdLibro().equals(reserva.getLibro().getIdLibro())) {
                exigirLibroSinDisponibles(libro);
            }
            exigirSinDuplicada(usuario.getIdUsuario(), libro.getIdLibro(), id);
        }

        reserva.setUsuario(usuario);
        reserva.setLibro(libro);
        reserva.setFechaVencimiento(r.getFechaVencimiento());
        reserva.setEstado(nuevo);
        return respuesta(reservaRepository.save(reserva));
    }

    /** Una reserva LISTA solo se puede dejar como está o cancelar (eso libera el ejemplar para el siguiente). */
    private ReservaResponse actualizarLista(Reserva reserva, ReservaRequest r) {
        EstadoReserva nuevo = (r.getEstado() == null) ? EstadoReserva.LISTA : r.getEstado();
        if (nuevo == EstadoReserva.CANCELADA) {
            colaReservas.cerrar(reserva, EstadoReserva.CANCELADA);
            return respuesta(reserva);
        }
        if (nuevo != EstadoReserva.LISTA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Una reserva lista para recoger solo se puede cancelar; se atiende registrando el préstamo");
        }
        return respuesta(reserva);
    }

    @Override
    @Transactional
    public ReservaResponse cancelar(Integer id) {
        colaReservas.vencerListas();
        Reserva reserva = buscarOFalla(id);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE && reserva.getEstado() != EstadoReserva.LISTA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede cancelar una reserva pendiente o lista para recoger");
        }
        // Si estaba LISTA, cerrar() libera el ejemplar apartado y se lo pasa al siguiente de la cola
        colaReservas.cerrar(reserva, EstadoReserva.CANCELADA);
        return respuesta(reserva);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Reserva reserva = buscarOFalla(id);
        if (reserva.getEstado() == EstadoReserva.LISTA) {
            colaReservas.cerrar(reserva, EstadoReserva.CANCELADA); // libera el ejemplar apartado
        }
        reservaRepository.delete(reserva);
    }

    // ---------------- helpers ----------------

    private Reserva buscarOFalla(Integer id) {
        return reservaRepository.buscarConDetalle(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
    }

    private Usuario usuarioOFalla(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no existe"));
    }

    private Libro libroOFalla(Integer idLibro) {
        return libroRepository.findById(idLibro)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El libro no existe"));
    }

    private void exigirUsuarioActivo(Usuario usuario) {
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario no puede reservar (estado: " + usuario.getEstado() + ")");
        }
    }

    private void exigirLibroActivo(Libro libro) {
        if (libro.getEstado() != EstadoLibro.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El libro no está activo en el catálogo");
        }
    }

    /** Se reserva solo cuando no hay ejemplar libre; si lo hay, lo que corresponde es registrar el préstamo. */
    private void exigirLibroSinDisponibles(Libro libro) {
        Integer idLibro = libro.getIdLibro();
        if (ejemplarRepository.countByLibroIdLibroAndEstado(idLibro, EstadoEjemplar.DISPONIBLE) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este libro tiene ejemplares disponibles: regístrale el préstamo en lugar de una reserva");
        }
        long enCirculacion = ejemplarRepository.countByLibroIdLibroAndEstado(idLibro, EstadoEjemplar.PRESTADO)
                + ejemplarRepository.countByLibroIdLibroAndEstado(idLibro, EstadoEjemplar.RESERVADO);
        if (enCirculacion == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este libro no tiene ejemplares prestados que puedan devolverse: no se puede reservar");
        }
    }

    private void exigirSinDuplicada(Integer idUsuario, Integer idLibro, Integer excluirId) {
        if (reservaRepository.contarVigentes(idUsuario, idLibro, LocalDate.now(), excluirId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este usuario ya tiene una reserva pendiente de ese libro");
        }
    }

    /** Por libro: cuántos ejemplares están DISPONIBLES ahora. */
    private Map<Integer, Long> disponiblesPorLibro() {
        Map<Integer, Long> mapa = new HashMap<>();
        for (Object[] fila : ejemplarRepository.contarPorLibro()) {
            long disponibles = (fila[2] == null) ? 0L : ((Number) fila[2]).longValue();
            mapa.put((Integer) fila[0], disponibles);
        }
        return mapa;
    }

    /** Turno de cada reserva PENDIENTE y vigente dentro de la cola de su libro (por orden de llegada). */
    private Map<Integer, Integer> posiciones(List<Reserva> todas) {
        LocalDate hoy = LocalDate.now();
        Map<Integer, List<Reserva>> porLibro = new HashMap<>();
        for (Reserva r : todas) {
            if (r.getEstado() == EstadoReserva.PENDIENTE && !r.getFechaVencimiento().isBefore(hoy)) {
                porLibro.computeIfAbsent(r.getLibro().getIdLibro(), k -> new java.util.ArrayList<>()).add(r);
            }
        }
        Map<Integer, Integer> posiciones = new HashMap<>();
        for (List<Reserva> cola : porLibro.values()) {
            cola.sort(Comparator.comparing(Reserva::getFechaReserva).thenComparing(Reserva::getIdReserva));
            for (int i = 0; i < cola.size(); i++) {
                posiciones.put(cola.get(i).getIdReserva(), i + 1);
            }
        }
        return posiciones;
    }

    private ReservaResponse respuesta(Reserva r) {
        return aRespuesta(r, disponiblesPorLibro(), posiciones(reservaRepository.listarTodas()));
    }

    private ReservaResponse aRespuesta(Reserva r, Map<Integer, Long> disponibles, Map<Integer, Integer> posiciones) {
        boolean vencida = r.getEstado() == EstadoReserva.PENDIENTE
                && r.getFechaVencimiento().isBefore(LocalDate.now());
        String estado = vencida ? EstadoReserva.VENCIDA.name() : r.getEstado().name();
        Usuario u = r.getUsuario();
        Libro l = r.getLibro();
        return ReservaResponse.builder()
                .idReserva(r.getIdReserva())
                .idUsuario(u.getIdUsuario())
                .usuario(u.getNombres() + " " + u.getApellidos())
                .documento(u.getDocumento())
                .idLibro(l.getIdLibro())
                .titulo(l.getTitulo())
                .isbn(l.getIsbn())
                .fechaReserva(r.getFechaReserva())
                .fechaVencimiento(r.getFechaVencimiento())
                .estado(estado)
                .ejemplaresDisponibles(disponibles.getOrDefault(l.getIdLibro(), 0L))
                .posicionCola(posiciones.get(r.getIdReserva()))
                .fechaLimiteRecogida(r.getEstado() == EstadoReserva.LISTA ? r.getFechaLimiteRecogida() : null)
                .codigoEjemplar(r.getEstado() == EstadoReserva.LISTA && r.getEjemplar() != null
                        ? r.getEjemplar().getCodigo() : null)
                .editable(r.getEstado() == EstadoReserva.PENDIENTE || r.getEstado() == EstadoReserva.LISTA)
                .build();
    }
}
