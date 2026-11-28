package com.biblosmast.service;

import com.biblosmast.model.dto.LibroPrestadoItem;
import com.biblosmast.model.dto.PrestamosMesItem;
import com.biblosmast.model.dto.UsuarioRetrasoItem;
import com.biblosmast.model.entity.Prestamo;
import com.biblosmast.repository.DetallePrestamoRepository;
import com.biblosmast.repository.MultaRepository;
import com.biblosmast.repository.PrestamoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReporteService implements IReporteService {

    private static final int MAX_LIMITE = 50;
    private static final int MAX_MESES = 24;

    private final DetallePrestamoRepository detalleRepository;
    private final MultaRepository multaRepository;
    private final PrestamoRepository prestamoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LibroPrestadoItem> librosMasPrestados(LocalDate desde, LocalDate hasta, int limite) {
        LocalDate[] rango = rango(desde, hasta);
        validarLimite(limite);
        return detalleRepository.masPrestados(rango[0].atStartOfDay(), rango[1].plusDays(1).atStartOfDay(),
                        PageRequest.of(0, limite)).stream()
                .map(f -> LibroPrestadoItem.builder()
                        .idLibro((Integer) f[0])
                        .titulo((String) f[1])
                        .prestamos((Long) f[2])
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioRetrasoItem> usuariosConRetrasos(LocalDate desde, LocalDate hasta, int limite) {
        LocalDate[] rango = rango(desde, hasta);
        validarLimite(limite);

        Map<Integer, UsuarioRetrasoItem.UsuarioRetrasoItemBuilder> porUsuario = new LinkedHashMap<>();
        for (Object[] f : multaRepository.retrasosPorUsuario(rango[0].atStartOfDay(), rango[1].plusDays(1).atStartOfDay())) {
            porUsuario.put((Integer) f[0], UsuarioRetrasoItem.builder()
                    .idUsuario((Integer) f[0])
                    .usuario(f[1] + " " + f[2])
                    .documento((String) f[3])
                    .multasRetraso((Long) f[4])
                    .valorMultasRetraso((BigDecimal) f[5])
                    .ejemplaresVencidosAhora(0));
        }
        // Lo que está vencido hoy (sin multa todavía) también cuenta: es el retraso que está ocurriendo ahora
        for (Object[] f : detalleRepository.vencidosPorUsuario(LocalDate.now())) {
            porUsuario.computeIfAbsent((Integer) f[0], id -> UsuarioRetrasoItem.builder()
                            .idUsuario(id)
                            .usuario(f[1] + " " + f[2])
                            .documento((String) f[3])
                            .multasRetraso(0)
                            .valorMultasRetraso(BigDecimal.ZERO))
                    .ejemplaresVencidosAhora((Long) f[4]);
        }
        return porUsuario.values().stream()
                .map(UsuarioRetrasoItem.UsuarioRetrasoItemBuilder::build)
                .sorted(Comparator
                        .comparingLong((UsuarioRetrasoItem i) -> i.getMultasRetraso() + i.getEjemplaresVencidosAhora())
                        .reversed()
                        .thenComparing(UsuarioRetrasoItem::getUsuario, String.CASE_INSENSITIVE_ORDER))
                .limit(limite)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrestamosMesItem> prestamosPorMes(int meses) {
        if (meses < 1 || meses > MAX_MESES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los meses deben estar entre 1 y " + MAX_MESES);
        }
        YearMonth actual = YearMonth.now();
        YearMonth primero = actual.minusMonths(meses - 1L);

        Map<YearMonth, Long> conteo = new LinkedHashMap<>();
        for (int i = 0; i < meses; i++) {
            conteo.put(primero.plusMonths(i), 0L);
        }
        for (Prestamo p : prestamoRepository.findByFechaPrestamoGreaterThanEqual(primero.atDay(1).atStartOfDay())) {
            conteo.merge(YearMonth.from(p.getFechaPrestamo()), 1L, Long::sum);
        }
        List<PrestamosMesItem> resultado = new ArrayList<>();
        conteo.forEach((mes, n) -> resultado.add(PrestamosMesItem.builder().mes(mes.toString()).prestamos(n).build()));
        return resultado;
    }

    /** Por defecto, los últimos 12 meses hasta hoy; el rango es inclusivo en ambos extremos. */
    private LocalDate[] rango(LocalDate desde, LocalDate hasta) {
        LocalDate fin = (hasta == null) ? LocalDate.now() : hasta;
        LocalDate inicio = (desde == null) ? fin.minusMonths(12) : desde;
        if (inicio.isAfter(fin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la final");
        }
        return new LocalDate[] {inicio, fin};
    }

    private void validarLimite(int limite) {
        if (limite < 1 || limite > MAX_LIMITE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El límite debe estar entre 1 y " + MAX_LIMITE);
        }
    }
}
