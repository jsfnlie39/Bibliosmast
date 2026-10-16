package com.biblosmast.service;

import com.biblosmast.model.dto.DetallePrestamoResponse;
import com.biblosmast.model.dto.DevolucionRequest;
import com.biblosmast.model.dto.PrestamoRequest;
import com.biblosmast.model.dto.PrestamoResponse;
import com.biblosmast.model.entity.Cuenta;
import com.biblosmast.model.entity.DetallePrestamo;
import com.biblosmast.model.entity.Ejemplar;
import com.biblosmast.model.entity.EstadoEjemplar;
import com.biblosmast.model.entity.EstadoPrestamo;
import com.biblosmast.model.entity.EstadoReserva;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.model.entity.Prestamo;
import com.biblosmast.model.entity.Reserva;
import com.biblosmast.model.entity.Usuario;
import com.biblosmast.repository.CuentaRepository;
import com.biblosmast.repository.EjemplarRepository;
import com.biblosmast.repository.PrestamoRepository;
import com.biblosmast.repository.ReservaRepository;
import com.biblosmast.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PrestamoService implements IPrestamoService {

    private static final Set<String> ROLES_PERSONAL = Set.of("ADMIN", "BIBLIOTECARIO");

    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final EjemplarRepository ejemplarRepository;
    private final ReservaRepository reservaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PrestamoResponse> listar(String estado, Integer idUsuario) {
        String filtro = (estado == null) ? "" : estado.trim().toUpperCase();
        return prestamoRepository.listarTodos().stream()
                .map(this::aRespuesta)
                .filter(p -> filtro.isEmpty() || p.getEstado().equals(filtro))
                .filter(p -> idUsuario == null || p.getIdUsuario().equals(idUsuario))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PrestamoResponse obtener(Integer id) {
        return aRespuesta(buscarOFalla(id));
    }

    @Override
    @Transactional
    public PrestamoResponse crear(PrestamoRequest r) {
        Usuario usuario = usuarioRepository.findById(r.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no existe"));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario no puede recibir préstamos (estado: " + usuario.getEstado() + ")");
        }

        Cuenta cuentaBibliotecario = cuentaRepository.buscarPorIdUsuario(r.getIdBibliotecario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El bibliotecario no existe"));
        if (!ROLES_PERSONAL.contains(cuentaBibliotecario.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el personal de la biblioteca registra préstamos");
        }

        if (!r.getFechaLimite().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha límite debe ser posterior a hoy");
        }

        Set<Integer> ids = new LinkedHashSet<>(r.getIdsEjemplares());
        List<Ejemplar> ejemplares = ejemplarRepository.bloquearPorIds(ids);
        if (ejemplares.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alguno de los ejemplares no existe");
        }
        String noDisponibles = ejemplares.stream()
                .filter(e -> e.getEstado() != EstadoEjemplar.DISPONIBLE)
                .map(Ejemplar::getCodigo)
                .collect(Collectors.joining(", "));
        if (!noDisponibles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ejemplares no disponibles: " + noDisponibles);
        }

        Usuario bibliotecario = cuentaBibliotecario.getUsuario();
        Prestamo prestamo = new Prestamo();
        prestamo.setUsuario(usuario);
        prestamo.setBibliotecario(bibliotecario);
        prestamo.setFechaPrestamo(LocalDateTime.now());
        prestamo.setFechaLimite(r.getFechaLimite());
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        prestamo.setObservaciones(r.getObservaciones() == null || r.getObservaciones().isBlank()
                ? null : r.getObservaciones().trim());

        for (Ejemplar ejemplar : ejemplares) {
            DetallePrestamo detalle = new DetallePrestamo();
            detalle.setPrestamo(prestamo);
            detalle.setEjemplar(ejemplar);
            prestamo.getDetalles().add(detalle);
        }

        // Al insertar cada detalle, el trigger de la BD marca el ejemplar como PRESTADO.
        Prestamo guardado = prestamoRepository.saveAndFlush(prestamo);

        // Si el usuario tenía reservado alguno de esos libros, la reserva queda atendida.
        int atendidas = atenderReservas(usuario, ejemplares);
        return aRespuesta(guardado, atendidas);
    }

    @Override
    @Transactional
    public PrestamoResponse devolver(Integer idPrestamo, Integer idDetalle, DevolucionRequest r) {
        Prestamo prestamo = buscarOFalla(idPrestamo);
        DetallePrestamo detalle = prestamo.getDetalles().stream()
                .filter(d -> d.getIdDetalle().equals(idDetalle))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El ejemplar no pertenece a este préstamo"));
        if (detalle.getFechaDevolucion() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este ejemplar ya fue devuelto");
        }

        detalle.setFechaDevolucion(LocalDateTime.now());
        detalle.setEstadoDevolucion(r.getEstadoDevolucion());

        boolean todosDevueltos = prestamo.getDetalles().stream().allMatch(d -> d.getFechaDevolucion() != null);
        if (todosDevueltos) {
            prestamo.setEstado(EstadoPrestamo.FINALIZADO);
        }

        // Al actualizar el detalle, el trigger de la BD deja el ejemplar DISPONIBLE, DANADO o PERDIDO.
        return aRespuesta(prestamoRepository.saveAndFlush(prestamo));
    }

    // ---------------- helpers ----------------

    private Prestamo buscarOFalla(Integer id) {
        return prestamoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Préstamo no encontrado"));
    }

    /** Las reservas pendientes y vigentes del usuario para los libros que se lleva pasan a ATENDIDA. */
    private int atenderReservas(Usuario usuario, List<Ejemplar> ejemplares) {
        Set<Integer> idsLibros = ejemplares.stream()
                .map(e -> e.getLibro().getIdLibro())
                .collect(Collectors.toSet());
        List<Reserva> reservas = reservaRepository.pendientesVigentes(usuario.getIdUsuario(), idsLibros, LocalDate.now());
        reservas.forEach(r -> r.setEstado(EstadoReserva.ATENDIDA));
        return reservas.size();
    }

    private PrestamoResponse aRespuesta(Prestamo p) {
        return aRespuesta(p, 0);
    }

    private PrestamoResponse aRespuesta(Prestamo p, int reservasAtendidas) {
        LocalDate hoy = LocalDate.now();
        boolean finalizado = p.getEstado() == EstadoPrestamo.FINALIZADO;
        boolean vencido = !finalizado && p.getFechaLimite().isBefore(hoy);
        String estado = finalizado ? "FINALIZADO" : (vencido ? "VENCIDO" : "ACTIVO");
        long diasRetraso = vencido ? ChronoUnit.DAYS.between(p.getFechaLimite(), hoy) : 0;

        List<DetallePrestamoResponse> detalles = p.getDetalles().stream()
                .map(d -> DetallePrestamoResponse.builder()
                        .idDetalle(d.getIdDetalle())
                        .idEjemplar(d.getEjemplar().getIdEjemplar())
                        .codigo(d.getEjemplar().getCodigo())
                        .titulo(d.getEjemplar().getLibro().getTitulo())
                        .fechaDevolucion(d.getFechaDevolucion())
                        .estadoDevolucion(d.getEstadoDevolucion() == null ? null : d.getEstadoDevolucion().name())
                        .build())
                .toList();

        return PrestamoResponse.builder()
                .idPrestamo(p.getIdPrestamo())
                .idUsuario(p.getUsuario().getIdUsuario())
                .usuario(p.getUsuario().getNombres() + " " + p.getUsuario().getApellidos())
                .documento(p.getUsuario().getDocumento())
                .bibliotecario(p.getBibliotecario().getNombres() + " " + p.getBibliotecario().getApellidos())
                .fechaPrestamo(p.getFechaPrestamo())
                .fechaLimite(p.getFechaLimite())
                .estado(estado)
                .diasRetraso(diasRetraso)
                .observaciones(p.getObservaciones())
                .detalles(detalles)
                .reservasAtendidas(reservasAtendidas)
                .build();
    }
}
