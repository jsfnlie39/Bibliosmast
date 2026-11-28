package com.biblosmast.service;

import com.biblosmast.model.dto.DetallePrestamoResponse;
import com.biblosmast.model.dto.DevolucionRequest;
import com.biblosmast.model.dto.PrestamoRequest;
import com.biblosmast.model.dto.PrestamoResponse;
import com.biblosmast.model.entity.Cuenta;
import com.biblosmast.model.entity.DetallePrestamo;
import com.biblosmast.model.entity.Ejemplar;
import com.biblosmast.model.entity.EstadoDevolucion;
import com.biblosmast.model.entity.EstadoEjemplar;
import com.biblosmast.model.entity.EstadoMulta;
import com.biblosmast.model.entity.EstadoPrestamo;
import com.biblosmast.model.entity.EstadoReserva;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.model.entity.MotivoMulta;
import com.biblosmast.model.entity.Multa;
import com.biblosmast.model.entity.Prestamo;
import com.biblosmast.model.entity.Reserva;
import com.biblosmast.model.entity.TipoUsuario;
import com.biblosmast.model.entity.Usuario;
import com.biblosmast.repository.CuentaRepository;
import com.biblosmast.repository.EjemplarRepository;
import com.biblosmast.repository.MultaRepository;
import com.biblosmast.repository.PrestamoRepository;
import com.biblosmast.repository.ReservaRepository;
import com.biblosmast.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

    /** Tope de una multa: lo que cabe en la columna DECIMAL(10,2). */
    private static final BigDecimal MULTA_MAXIMA = new BigDecimal("99999999.99");

    private static final Set<String> ROLES_PERSONAL = Set.of("ADMIN", "BIBLIOTECARIO");

    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final EjemplarRepository ejemplarRepository;
    private final ReservaRepository reservaRepository;
    private final MultaRepository multaRepository;
    private final IConfiguracionService configuracionService;
    private final IColaReservasService colaReservas;

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
    public PrestamoResponse crear(PrestamoRequest r, String usernameBibliotecario) {
        colaReservas.vencerListas(); // libera los ejemplares apartados que nadie recogió a tiempo
        Usuario usuario = usuarioRepository.findById(r.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no existe"));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario no puede recibir préstamos (estado: " + usuario.getEstado() + ")");
        }

        if (multaRepository.existsByUsuarioIdUsuarioAndEstado(usuario.getIdUsuario(), EstadoMulta.PENDIENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario tiene multas pendientes: debe pagarlas antes de recibir un préstamo");
        }

        // Quien registra el préstamo es la persona con sesión: sale del token, no de lo que envíe el navegador
        Cuenta cuentaBibliotecario = cuentaRepository.buscarPorUsername(usernameBibliotecario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));
        if (!ROLES_PERSONAL.contains(cuentaBibliotecario.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el personal de la biblioteca registra préstamos");
        }

        // Las reglas dependen del tipo de usuario: días del préstamo y máximo de libros sin devolver
        TipoUsuario tipo = usuario.getTipoUsuario();
        LocalDate hoy = LocalDate.now();
        LocalDate fechaMaxima = hoy.plusDays(tipo.getDiasPrestamo());
        LocalDate fechaLimite = (r.getFechaLimite() == null) ? fechaMaxima : r.getFechaLimite();
        if (!fechaLimite.isAfter(hoy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha límite debe ser posterior a hoy");
        }
        if (fechaLimite.isAfter(fechaMaxima)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un usuario " + tipo.getNombre() + " tiene máximo " + tipo.getDiasPrestamo()
                            + " días de préstamo: la fecha límite no puede pasar del " + fechaMaxima);
        }

        Set<Integer> ids = new LinkedHashSet<>(r.getIdsEjemplares());

        long sinDevolver = prestamoRepository.contarEjemplaresSinDevolver(usuario.getIdUsuario());
        if (sinDevolver + ids.size() > tipo.getMaxLibros()) {
            long puedeLlevar = Math.max(0, tipo.getMaxLibros() - sinDevolver);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un usuario " + tipo.getNombre() + " puede tener máximo " + tipo.getMaxLibros()
                            + " libros sin devolver y este ya tiene " + sinDevolver
                            + (puedeLlevar == 0 ? ": debe devolver alguno antes de llevarse más"
                                    : ": solo puede llevarse " + puedeLlevar + " más"));
        }
        List<Ejemplar> ejemplares = ejemplarRepository.bloquearPorIds(ids);
        if (ejemplares.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alguno de los ejemplares no existe");
        }
        // Un ejemplar apartado (RESERVADO) solo se le presta a quien lo tiene reservado y listo para recoger
        List<Reserva> listasDelUsuario = reservaRepository.listasDeUsuario(usuario.getIdUsuario());
        Set<Integer> apartadosParaElUsuario = listasDelUsuario.stream()
                .map(rv -> rv.getEjemplar().getIdEjemplar())
                .collect(Collectors.toSet());
        String noDisponibles = ejemplares.stream()
                .filter(e -> !(e.getEstado() == EstadoEjemplar.DISPONIBLE
                        || (e.getEstado() == EstadoEjemplar.RESERVADO
                                && apartadosParaElUsuario.contains(e.getIdEjemplar()))))
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
        prestamo.setFechaLimite(fechaLimite);
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
        int atendidas = atenderReservas(usuario, ejemplares, listasDelUsuario);
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

        LocalDateTime ahora = LocalDateTime.now();
        detalle.setFechaDevolucion(ahora);
        detalle.setEstadoDevolucion(r.getEstadoDevolucion());

        // Multas por retraso, daño o pérdida (los valores los define el administrador en la configuración)
        List<Multa> multas = calcularMultas(prestamo, r.getEstadoDevolucion(), ahora);
        multaRepository.saveAll(multas);
        BigDecimal totalMultas = multas.stream().map(Multa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean todosDevueltos = prestamo.getDetalles().stream().allMatch(d -> d.getFechaDevolucion() != null);
        if (todosDevueltos) {
            prestamo.setEstado(EstadoPrestamo.FINALIZADO);
        }

        // Al actualizar el detalle, el trigger de la BD deja el ejemplar DISPONIBLE, DANADO o PERDIDO.
        Prestamo guardado = prestamoRepository.saveAndFlush(prestamo);

        // Si el ejemplar volvió en buen estado y hay cola de reservas, se aparta para el siguiente
        String apartadoPara = null;
        if (r.getEstadoDevolucion() == EstadoDevolucion.BUEN_ESTADO) {
            List<Reserva> listas = colaReservas.asignarEjemplarLibre(detalle.getEjemplar().getLibro().getIdLibro());
            if (!listas.isEmpty()) {
                Usuario espera = listas.get(0).getUsuario();
                apartadoPara = espera.getNombres() + " " + espera.getApellidos();
            }
        }
        return aRespuesta(guardado, 0, multas.size(), totalMultas, apartadoPara);
    }

    @Override
    @Transactional
    public PrestamoResponse renovar(Integer idPrestamo) {
        Prestamo prestamo = buscarOFalla(idPrestamo);
        LocalDate hoy = LocalDate.now();

        boolean finalizado = prestamo.getEstado() == EstadoPrestamo.FINALIZADO
                || prestamo.getDetalles().stream().allMatch(d -> d.getFechaDevolucion() != null);
        if (finalizado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede renovar: el préstamo ya fue devuelto");
        }
        if (prestamo.getFechaLimite().isBefore(hoy)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede renovar: el préstamo está vencido. Debe devolverse");
        }

        TipoUsuario tipo = prestamo.getUsuario().getTipoUsuario();
        int maximo = tipo.getMaxRenovaciones();
        if (prestamo.getRenovaciones() >= maximo) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede renovar: un usuario " + tipo.getNombre() + " tiene "
                            + (maximo == 0 ? "las renovaciones desactivadas" : "máximo " + maximo + " renovaciones")
                            + (prestamo.getRenovaciones() > 0 ? " y ya las usó" : ""));
        }

        Integer idUsuario = prestamo.getUsuario().getIdUsuario();
        if (multaRepository.existsByUsuarioIdUsuarioAndEstado(idUsuario, EstadoMulta.PENDIENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede renovar: el usuario tiene multas pendientes de pago");
        }

        // Solo cuentan los libros que siguen prestados (los ya devueltos no se renuevan)
        Set<Integer> idsLibros = prestamo.getDetalles().stream()
                .filter(d -> d.getFechaDevolucion() == null)
                .map(d -> d.getEjemplar().getLibro().getIdLibro())
                .collect(Collectors.toSet());
        if (reservaRepository.contarPendientesDeOtros(idUsuario, idsLibros, hoy) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede renovar: otra persona tiene reservado alguno de estos libros");
        }

        // La nueva fecha parte de la mayor entre la actual y hoy, y suma los días de préstamo
        int dias = tipo.getDiasPrestamo();
        LocalDate base = prestamo.getFechaLimite().isAfter(hoy) ? prestamo.getFechaLimite() : hoy;
        prestamo.setFechaLimite(base.plusDays(dias));
        prestamo.setRenovaciones(prestamo.getRenovaciones() + 1);

        return aRespuesta(prestamoRepository.saveAndFlush(prestamo));
    }

    // ---------------- helpers ----------------

    /**
     * Perdido: multa fija de pérdida (sin multa por retraso).
     * Dañado: multa fija de daño, más retraso si se devolvió tarde.
     * Buen estado: solo retraso si se devolvió después de la fecha límite (días de retraso x valor por día).
     */
    private List<Multa> calcularMultas(Prestamo prestamo, EstadoDevolucion estado, LocalDateTime ahora) {
        List<Multa> multas = new ArrayList<>();
        if (estado == EstadoDevolucion.PERDIDO) {
            agregarMulta(multas, prestamo, MotivoMulta.PERDIDA,
                    configuracionService.valor(ConfiguracionService.CLAVE_MULTA_PERDIDA), ahora);
            return multas;
        }
        if (estado == EstadoDevolucion.DANADO) {
            agregarMulta(multas, prestamo, MotivoMulta.DANO,
                    configuracionService.valor(ConfiguracionService.CLAVE_MULTA_DANO), ahora);
        }
        long diasRetraso = ChronoUnit.DAYS.between(prestamo.getFechaLimite(), ahora.toLocalDate());
        if (diasRetraso > 0) {
            BigDecimal porDia = configuracionService.valor(ConfiguracionService.CLAVE_MULTA_RETRASO);
            agregarMulta(multas, prestamo, MotivoMulta.RETRASO, porDia.multiply(BigDecimal.valueOf(diasRetraso)), ahora);
        }
        return multas;
    }

    /** No crea multas de valor cero (si el administrador deja un valor en 0, ese concepto no se cobra). */
    private void agregarMulta(List<Multa> multas, Prestamo prestamo, MotivoMulta motivo,
                              BigDecimal valor, LocalDateTime ahora) {
        if (valor.signum() <= 0) {
            return;
        }
        Multa multa = new Multa();
        multa.setPrestamo(prestamo);
        multa.setUsuario(prestamo.getUsuario());
        multa.setValor(valor.min(MULTA_MAXIMA).setScale(2, RoundingMode.HALF_UP));
        multa.setMotivo(motivo);
        multa.setEstado(EstadoMulta.PENDIENTE);
        multa.setFecha(ahora);
        multas.add(multa);
    }

    private Prestamo buscarOFalla(Integer id) {
        return prestamoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Préstamo no encontrado"));
    }

    /**
     * Las reservas del usuario para los libros que se lleva pasan a ATENDIDA: las LISTA (si se lleva otro ejemplar
     * distinto al apartado, el apartado queda libre para el siguiente de la cola) y las PENDIENTE vigentes.
     */
    private int atenderReservas(Usuario usuario, List<Ejemplar> ejemplares, List<Reserva> listasDelUsuario) {
        Set<Integer> idsLibros = ejemplares.stream()
                .map(e -> e.getLibro().getIdLibro())
                .collect(Collectors.toSet());
        Set<Integer> idsEjemplares = ejemplares.stream()
                .map(Ejemplar::getIdEjemplar)
                .collect(Collectors.toSet());

        int atendidas = 0;
        for (Reserva lista : listasDelUsuario) {
            if (idsLibros.contains(lista.getEjemplar().getLibro().getIdLibro())) {
                colaReservas.atender(lista, idsEjemplares);
                atendidas++;
            }
        }
        List<Reserva> pendientes = reservaRepository.pendientesVigentes(usuario.getIdUsuario(), idsLibros, LocalDate.now());
        pendientes.forEach(r -> r.setEstado(EstadoReserva.ATENDIDA));
        return atendidas + pendientes.size();
    }

    private PrestamoResponse aRespuesta(Prestamo p) {
        return aRespuesta(p, 0, 0, BigDecimal.ZERO);
    }

    private PrestamoResponse aRespuesta(Prestamo p, int reservasAtendidas) {
        return aRespuesta(p, reservasAtendidas, 0, BigDecimal.ZERO);
    }

    private PrestamoResponse aRespuesta(Prestamo p, int reservasAtendidas, int multasGeneradas, BigDecimal valorMultas) {
        return aRespuesta(p, reservasAtendidas, multasGeneradas, valorMultas, null);
    }

    private PrestamoResponse aRespuesta(Prestamo p, int reservasAtendidas, int multasGeneradas, BigDecimal valorMultas,
                                        String apartadoPara) {
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
                .renovaciones(p.getRenovaciones())
                .detalles(detalles)
                .reservasAtendidas(reservasAtendidas)
                .multasGeneradas(multasGeneradas)
                .valorMultasGeneradas(valorMultas)
                .apartadoPara(apartadoPara)
                .build();
    }
}
