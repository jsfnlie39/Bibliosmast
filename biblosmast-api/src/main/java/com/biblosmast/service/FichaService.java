package com.biblosmast.service;

import com.biblosmast.model.dto.EjemplarResponse;
import com.biblosmast.model.dto.FichaLibroResponse;
import com.biblosmast.model.dto.FichaUsuarioResponse;
import com.biblosmast.model.dto.MovimientoLibroResponse;
import com.biblosmast.model.dto.MultaResponse;
import com.biblosmast.model.entity.DetallePrestamo;
import com.biblosmast.model.entity.TipoUsuario;
import com.biblosmast.model.entity.Usuario;
import com.biblosmast.repository.DetallePrestamoRepository;
import com.biblosmast.repository.PrestamoRepository;
import com.biblosmast.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FichaService implements IFichaService {

    private final ILibroService libroService;
    private final IEjemplarService ejemplarService;
    private final IReservaService reservaService;
    private final IPrestamoService prestamoService;
    private final IMultaService multaService;
    private final DetallePrestamoRepository detalleRepository;
    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final IUsuarioService usuarioService;

    // No es de solo lectura: al listar reservas se liberan las que nadie recogió a tiempo
    @Override
    @Transactional
    public FichaLibroResponse libro(Integer idLibro) {
        var libro = libroService.obtener(idLibro); // 404 si no existe
        List<EjemplarResponse> ejemplares = ejemplarService.listar(idLibro);
        List<MovimientoLibroResponse> prestamos = detalleRepository.historialDeLibro(idLibro).stream()
                .map(this::aMovimiento)
                .toList();
        return FichaLibroResponse.builder()
                .libro(libro)
                .ejemplares(ejemplares)
                .totalPrestamos(prestamos.size())
                .prestamosSinDevolver(prestamos.stream().filter(m -> m.getFechaDevolucion() == null).count())
                .prestamos(prestamos)
                .reservas(reservaService.listarPorLibro(idLibro))
                .build();
    }

    @Override
    @Transactional
    public FichaUsuarioResponse usuario(Integer idUsuario) {
        var usuarioResponse = usuarioService.obtenerConBajas(idUsuario); // 404 si no existe; incluye a los dados de baja
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        TipoUsuario tipo = usuario.getTipoUsuario();

        List<MultaResponse> multas = multaService.listar(null, idUsuario);
        List<MultaResponse> pendientes = multas.stream().filter(m -> "PENDIENTE".equals(m.getEstado())).toList();
        BigDecimal valorPendiente = pendientes.stream().map(MultaResponse::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        return FichaUsuarioResponse.builder()
                .usuario(usuarioResponse)
                .maxLibros(tipo.getMaxLibros())
                .diasPrestamo(tipo.getDiasPrestamo())
                .maxRenovaciones(tipo.getMaxRenovaciones())
                .ejemplaresSinDevolver(prestamoRepository.contarEjemplaresSinDevolver(idUsuario))
                .multasPendientes(pendientes.size())
                .valorMultasPendientes(valorPendiente)
                .prestamos(prestamoService.listar(null, idUsuario))
                .reservas(reservaService.listar(null, idUsuario))
                .multas(multas)
                .build();
    }

    private MovimientoLibroResponse aMovimiento(DetallePrestamo d) {
        var prestamo = d.getPrestamo();
        var usuario = prestamo.getUsuario();
        boolean sinDevolver = d.getFechaDevolucion() == null;
        return MovimientoLibroResponse.builder()
                .idPrestamo(prestamo.getIdPrestamo())
                .idEjemplar(d.getEjemplar().getIdEjemplar())
                .codigoEjemplar(d.getEjemplar().getCodigo())
                .idUsuario(usuario.getIdUsuario())
                .usuario(usuario.getNombres() + " " + usuario.getApellidos())
                .documento(usuario.getDocumento())
                .fechaPrestamo(prestamo.getFechaPrestamo())
                .fechaLimite(prestamo.getFechaLimite())
                .fechaDevolucion(d.getFechaDevolucion())
                .estadoDevolucion(d.getEstadoDevolucion() == null ? null : d.getEstadoDevolucion().name())
                .vencido(sinDevolver && prestamo.getFechaLimite().isBefore(LocalDate.now()))
                .build();
    }
}
