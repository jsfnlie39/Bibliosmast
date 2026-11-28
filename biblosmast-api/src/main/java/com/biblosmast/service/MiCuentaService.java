package com.biblosmast.service;

import com.biblosmast.model.dto.MultaResponse;
import com.biblosmast.model.dto.PrestamoResponse;
import com.biblosmast.model.dto.ReservaRequest;
import com.biblosmast.model.dto.ReservaResponse;
import com.biblosmast.repository.CuentaRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MiCuentaService implements IMiCuentaService {

    /** Días de vigencia de una reserva hecha desde «Mi cuenta» (la cola puede tardar más que una reserva del mostrador). */
    static final int DIAS_VIGENCIA_RESERVA = 14;

    private final CuentaRepository cuentaRepository;
    private final IPrestamoService prestamoService;
    private final IReservaService reservaService;
    private final IMultaService multaService;

    @Override
    @Transactional(readOnly = true)
    public List<PrestamoResponse> prestamos(String username) {
        return prestamoService.listar(null, idUsuario(username));
    }

    @Override
    @Transactional
    public List<ReservaResponse> reservas(String username) {
        return reservaService.listar(null, idUsuario(username));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MultaResponse> multas(String username) {
        return multaService.listar(null, idUsuario(username));
    }

    @Override
    @Transactional
    public ReservaResponse reservar(String username, Integer idLibro) {
        ReservaRequest solicitud = new ReservaRequest();
        solicitud.setIdUsuario(idUsuario(username));
        solicitud.setIdLibro(idLibro);
        solicitud.setFechaVencimiento(LocalDate.now().plusDays(DIAS_VIGENCIA_RESERVA));
        return reservaService.crear(solicitud);
    }

    @Override
    @Transactional
    public ReservaResponse cancelarReserva(String username, Integer idReserva) {
        return reservaService.cancelarPropia(idReserva, idUsuario(username));
    }

    /** La persona sale del token: username -> cuenta -> usuario. */
    private Integer idUsuario(String username) {
        return cuentaRepository.buscarPorUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"))
                .getUsuario().getIdUsuario();
    }
}
