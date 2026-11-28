package com.biblosmast.service;

import com.biblosmast.model.dto.NotificacionResponse;
import com.biblosmast.model.entity.Notificacion;
import com.biblosmast.model.entity.TipoNotificacion;
import com.biblosmast.model.entity.Usuario;
import com.biblosmast.repository.CuentaRepository;
import com.biblosmast.repository.NotificacionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService implements INotificacionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificacionRepository notificacionRepository;
    private final CuentaRepository cuentaRepository;
    private final ICorreoService correoService;

    /** Se apaga con biblosmast.avisos.correo=false (los avisos dentro del sistema se crean igual). */
    @Value("${biblosmast.avisos.correo:true}")
    private boolean enviarCorreo;

    /** dd/MM/yyyy, para los textos de los avisos. */
    public static String fecha(LocalDate fecha) {
        return FECHA.format(fecha);
    }

    @Override
    @Transactional
    public boolean crearUnica(Usuario usuario, TipoNotificacion tipo, String mensaje, Integer idPrestamo,
                              Integer idReserva, String claveUnica) {
        if (notificacionRepository.existsByClaveUnica(claveUnica)) {
            return false;
        }
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setTipo(tipo);
        n.setMensaje(mensaje.length() > 300 ? mensaje.substring(0, 297) + "..." : mensaje);
        n.setIdPrestamo(idPrestamo);
        n.setIdReserva(idReserva);
        n.setFechaCreacion(LocalDateTime.now());
        n.setClaveUnica(claveUnica);
        notificacionRepository.save(n);

        if (enviarCorreo && usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            try {
                correoService.enviar(usuario.getEmail(), asunto(tipo), mensaje);
            } catch (RuntimeException e) {
                // Un fallo del correo no debe impedir el aviso dentro del sistema
                log.error("No se pudo enviar el aviso por correo a {}: {}", usuario.getEmail(), e.getMessage());
            }
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(String username) {
        return notificacionRepository
                .findTop30ByUsuarioIdUsuarioOrderByFechaCreacionDescIdNotificacionDesc(idUsuario(username)).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidas(String username) {
        return notificacionRepository.countByUsuarioIdUsuarioAndLeidaFalse(idUsuario(username));
    }

    @Override
    @Transactional
    public NotificacionResponse marcarLeida(String username, Integer idNotificacion) {
        // Un aviso ajeno responde igual que uno inexistente
        Notificacion n = notificacionRepository.findByIdNotificacionAndUsuarioIdUsuario(idNotificacion, idUsuario(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aviso no encontrado"));
        n.setLeida(true);
        return aRespuesta(notificacionRepository.save(n));
    }

    @Override
    @Transactional
    public int marcarTodasLeidas(String username) {
        return notificacionRepository.marcarTodasLeidas(idUsuario(username));
    }

    private String asunto(TipoNotificacion tipo) {
        return switch (tipo) {
            case POR_VENCER -> "Biblosmast: tu préstamo está por vencer";
            case VENCIDO -> "Biblosmast: tienes un préstamo vencido";
            case RESERVA_LISTA -> "Biblosmast: tu reserva está lista para recoger";
        };
    }

    private Integer idUsuario(String username) {
        return cuentaRepository.buscarPorUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"))
                .getUsuario().getIdUsuario();
    }

    private NotificacionResponse aRespuesta(Notificacion n) {
        return NotificacionResponse.builder()
                .idNotificacion(n.getIdNotificacion())
                .tipo(n.getTipo().name())
                .mensaje(n.getMensaje())
                .fechaCreacion(n.getFechaCreacion())
                .leida(Boolean.TRUE.equals(n.getLeida()))
                .build();
    }
}
