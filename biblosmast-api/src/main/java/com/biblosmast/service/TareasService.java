package com.biblosmast.service;

import com.biblosmast.model.dto.ResumenTareasResponse;
import com.biblosmast.model.entity.EstadoPrestamo;
import com.biblosmast.model.entity.EstadoReserva;
import com.biblosmast.model.entity.Prestamo;
import com.biblosmast.model.entity.TipoNotificacion;
import com.biblosmast.repository.PrestamoRepository;
import com.biblosmast.repository.ReservaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TareasService implements ITareasService {

    private final IColaReservasService colaReservas;
    private final PrestamoRepository prestamoRepository;
    private final ReservaRepository reservaRepository;
    private final INotificacionService notificaciones;

    /** Con cuántos días de anticipación se avisa que un préstamo está por vencer (incluye el mismo día). */
    @Value("${biblosmast.avisos.dias-anticipacion:2}")
    private int diasAnticipacion;

    @Override
    @Transactional
    public ResumenTareasResponse ejecutar() {
        LocalDate hoy = LocalDate.now();

        int reservasVencidas = colaReservas.vencerListas();
        long prestamosVencidos = prestamoRepository.countByEstadoAndFechaLimiteBefore(EstadoPrestamo.ACTIVO, hoy);
        long vencenManana = prestamoRepository.countByEstadoAndFechaLimite(EstadoPrestamo.ACTIVO, hoy.plusDays(1));
        long recogidasHoy = reservaRepository.countByEstadoAndFechaLimiteRecogida(EstadoReserva.LISTA, hoy);

        int avisos = crearAvisosDePrestamos(hoy);

        log.info("Tareas diarias: {} reserva(s) lista(s) vencida(s); {} préstamo(s) vencido(s); "
                        + "{} vence(n) mañana; {} reserva(s) lista(s) vence(n) hoy; {} aviso(s) nuevo(s)",
                reservasVencidas, prestamosVencidos, vencenManana, recogidasHoy, avisos);

        return ResumenTareasResponse.builder()
                .ejecutadoEn(LocalDateTime.now())
                .reservasListasVencidas(reservasVencidas)
                .prestamosVencidos(prestamosVencidos)
                .prestamosQueVencenManana(vencenManana)
                .reservasListasQueVencenHoy(recogidasHoy)
                .avisosCreados(avisos)
                .build();
    }

    /**
     * Avisa a quienes tienen préstamos por vencer (hoy o en los próximos días) o ya vencidos. Cada aviso se crea una
     * sola vez por préstamo y fecha límite: si se renueva, la nueva fecha puede avisar de nuevo.
     */
    private int crearAvisosDePrestamos(LocalDate hoy) {
        int creados = 0;
        for (Prestamo p : prestamoRepository.findByEstadoAndFechaLimiteBetween(
                EstadoPrestamo.ACTIVO, hoy, hoy.plusDays(diasAnticipacion))) {
            long faltan = ChronoUnit.DAYS.between(hoy, p.getFechaLimite());
            String cuando = faltan == 0 ? "vence hoy" : faltan == 1 ? "vence mañana" : "vence en " + faltan + " días";
            String mensaje = "Tu préstamo (" + titulosSinDevolver(p) + ") " + cuando + ": devuélvelo o renuévalo en la "
                    + "biblioteca antes del " + NotificacionService.fecha(p.getFechaLimite()) + ".";
            if (notificaciones.crearUnica(p.getUsuario(), TipoNotificacion.POR_VENCER, mensaje, p.getIdPrestamo(), null,
                    "POR_VENCER:" + p.getIdPrestamo() + ":" + p.getFechaLimite())) {
                creados++;
            }
        }
        for (Prestamo p : prestamoRepository.findByEstadoAndFechaLimiteBefore(EstadoPrestamo.ACTIVO, hoy)) {
            String mensaje = "Tu préstamo (" + titulosSinDevolver(p) + ") venció el "
                    + NotificacionService.fecha(p.getFechaLimite()) + ". Devuélvelo cuanto antes: cada día de retraso "
                    + "genera una multa.";
            if (notificaciones.crearUnica(p.getUsuario(), TipoNotificacion.VENCIDO, mensaje, p.getIdPrestamo(), null,
                    "VENCIDO:" + p.getIdPrestamo() + ":" + p.getFechaLimite())) {
                creados++;
            }
        }
        return creados;
    }

    private String titulosSinDevolver(Prestamo p) {
        return p.getDetalles().stream()
                .filter(d -> d.getFechaDevolucion() == null)
                .map(d -> d.getEjemplar().getLibro().getTitulo())
                .distinct()
                .collect(Collectors.joining(", "));
    }
}
