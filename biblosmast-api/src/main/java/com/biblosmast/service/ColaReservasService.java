package com.biblosmast.service;

import com.biblosmast.model.entity.Ejemplar;
import com.biblosmast.model.entity.EstadoReserva;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.model.entity.Reserva;
import com.biblosmast.model.entity.TipoNotificacion;
import com.biblosmast.repository.EjemplarRepository;
import com.biblosmast.repository.ReservaRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ColaReservasService implements IColaReservasService {

    private final ReservaRepository reservaRepository;
    private final EjemplarRepository ejemplarRepository;
    private final IConfiguracionService configuracionService;
    private final INotificacionService notificaciones;

    @Override
    @Transactional
    public List<Reserva> asignarEjemplarLibre(Integer idLibro) {
        LocalDate hoy = LocalDate.now();
        List<Integer> libres = new ArrayList<>(ejemplarRepository.idsDisponiblesDeLibro(idLibro));
        List<Reserva> listas = new ArrayList<>();
        if (libres.isEmpty()) {
            return listas;
        }
        int diasRecogida = configuracionService.valor(ConfiguracionService.CLAVE_DIAS_RECOGIDA).intValue();

        for (Reserva reserva : reservaRepository.colaDeLibro(idLibro, hoy)) {
            if (libres.isEmpty()) {
                break;
            }
            // Un usuario dado de baja o bloqueado no recoge nada: se salta y sigue el siguiente
            if (reserva.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
                continue;
            }
            Integer idEjemplar = libres.remove(0);
            if (ejemplarRepository.apartar(idEjemplar) != 1) {
                continue; // alguien lo tomó en el mismo instante
            }
            Ejemplar ejemplar = ejemplarRepository.findById(idEjemplar).orElseThrow();
            reserva.setEjemplar(ejemplar);
            reserva.setEstado(EstadoReserva.LISTA);
            reserva.setFechaLimiteRecogida(hoy.plusDays(diasRecogida));
            listas.add(reserva);
        }
        if (!listas.isEmpty()) {
            reservaRepository.saveAll(listas);
            for (Reserva reserva : listas) {
                avisarReservaLista(reserva);
            }
        }
        return listas;
    }

    @Override
    @Transactional
    public int vencerListas() {
        List<Reserva> vencidas = reservaRepository.listasVencidas(LocalDate.now());
        if (vencidas.isEmpty()) {
            return 0;
        }
        Set<Integer> libros = new LinkedHashSet<>();
        for (Reserva reserva : vencidas) {
            libros.add(reserva.getLibro().getIdLibro());
            liberarEjemplar(reserva);
            reserva.setEstado(EstadoReserva.VENCIDA);
        }
        reservaRepository.saveAll(vencidas);
        reservaRepository.flush();
        libros.forEach(this::asignarEjemplarLibre);
        return vencidas.size();
    }

    @Override
    @Transactional
    public void cerrar(Reserva reserva, EstadoReserva nuevoEstado) {
        cerrarInterno(reserva, nuevoEstado, false);
    }

    @Override
    @Transactional
    public void atender(Reserva reserva, Collection<Integer> idsEjemplaresEntregados) {
        Ejemplar apartado = reserva.getEjemplar();
        boolean seLlevoElApartado = apartado != null && idsEjemplaresEntregados.contains(apartado.getIdEjemplar());
        cerrarInterno(reserva, EstadoReserva.ATENDIDA, seLlevoElApartado);
    }

    private void cerrarInterno(Reserva reserva, EstadoReserva nuevoEstado, boolean conservarEjemplar) {
        boolean liberar = reserva.getEstado() == EstadoReserva.LISTA && !conservarEjemplar;
        Integer idLibro = reserva.getLibro().getIdLibro();
        if (liberar) {
            liberarEjemplar(reserva);
        }
        reserva.setEstado(nuevoEstado);
        reservaRepository.saveAndFlush(reserva);
        if (liberar) {
            asignarEjemplarLibre(idLibro);
        }
    }

    /** Avisa a la persona que su reserva ya está lista (dentro del sistema y por correo). Una sola vez por reserva y plazo. */
    private void avisarReservaLista(Reserva reserva) {
        String hasta = NotificacionService.fecha(reserva.getFechaLimiteRecogida());
        String mensaje = "Tu reserva de «" + reserva.getLibro().getTitulo() + "» ya está lista: recoge el ejemplar "
                + reserva.getEjemplar().getCodigo() + " en la biblioteca antes del " + hasta + ".";
        notificaciones.crearUnica(reserva.getUsuario(), TipoNotificacion.RESERVA_LISTA, mensaje, null,
                reserva.getIdReserva(), "RESERVA_LISTA:" + reserva.getIdReserva() + ":" + reserva.getFechaLimiteRecogida());
    }

    /** Devuelve a DISPONIBLE el ejemplar que tenía apartado la reserva. */
    private void liberarEjemplar(Reserva reserva) {
        Ejemplar ejemplar = reserva.getEjemplar();
        if (ejemplar != null) {
            ejemplarRepository.liberar(ejemplar.getIdEjemplar());
            reserva.setEjemplar(null);
            reserva.setFechaLimiteRecogida(null);
        }
    }
}
