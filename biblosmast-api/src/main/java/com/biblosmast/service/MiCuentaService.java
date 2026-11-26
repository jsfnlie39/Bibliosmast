package com.biblosmast.service;

import com.biblosmast.model.dto.MiCuentaResponse;
import com.biblosmast.model.dto.MultaResponse;
import com.biblosmast.model.dto.PrestamoResponse;
import com.biblosmast.model.dto.ReservaRequest;
import com.biblosmast.model.dto.ReservaResponse;
import com.biblosmast.repository.CuentaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MiCuentaService implements IMiCuentaService {

    private final CuentaRepository cuentaRepository;
    private final IPrestamoService prestamoService;
    private final IReservaService reservaService;
    private final IMultaService multaService;
    private final IConfiguracionService configuracionService;

    @Override
    public MiCuentaResponse resumen(String username) {
        Integer idUsuario = idUsuario(username);
        List<PrestamoResponse> prestamos = prestamoService.listar(null, idUsuario);
        List<ReservaResponse> reservas = reservaService.listar(null, idUsuario);
        List<MultaResponse> multas = multaService.listar(null, idUsuario);
        BigDecimal pendientes = multas.stream()
                .filter(m -> "PENDIENTE".equals(m.getEstado()))
                .map(MultaResponse::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return MiCuentaResponse.builder()
                .idUsuario(idUsuario)
                .prestamos(prestamos)
                .reservas(reservas)
                .multas(multas)
                .totalMultasPendientes(pendientes)
                .build();
    }

    @Override
    public ReservaResponse reservar(String username, Integer idLibro) {
        ReservaRequest solicitud = new ReservaRequest();
        solicitud.setIdUsuario(idUsuario(username));
        solicitud.setIdLibro(idLibro);
        int dias = Math.max(2, configuracionService.valor(ConfiguracionService.CLAVE_DIAS_VIGENCIA_RESERVA).intValue());
        solicitud.setFechaVencimiento(LocalDate.now().plusDays(dias));
        return reservaService.crear(solicitud);
    }

    @Override
    public ReservaResponse cancelarReserva(String username, Integer idReserva) {
        Integer idUsuario = idUsuario(username);
        ReservaResponse reserva = reservaService.obtener(idReserva);
        if (!reserva.getIdUsuario().equals(idUsuario)) {
            // No se revela que la reserva existe si es de otra persona
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada");
        }
        return reservaService.cancelar(idReserva);
    }

    @Override
    public PrestamoResponse renovarPrestamo(String username, Integer idPrestamo) {
        Integer idUsuario = idUsuario(username);
        PrestamoResponse prestamo = prestamoService.obtener(idPrestamo);
        if (!prestamo.getIdUsuario().equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Préstamo no encontrado");
        }
        return prestamoService.renovar(idPrestamo);
    }

    private Integer idUsuario(String username) {
        return cuentaRepository.buscarPorUsername(username)
                .map(c -> c.getUsuario().getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida"));
    }
}
