package com.biblosmast.service;

import com.biblosmast.model.dto.AuditoriaResponse;
import com.biblosmast.model.entity.Auditoria;
import com.biblosmast.model.entity.Usuario;
import com.biblosmast.repository.AuditoriaRepository;
import com.biblosmast.repository.CuentaRepository;
import com.biblosmast.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService implements IAuditoriaService {

    private static final int LIMITE_POR_DEFECTO = 200;
    private static final int LIMITE_MAXIMO = 500;
    private static final int DIAS_POR_DEFECTO = 7;

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Integer idUsuario, String accion, String tabla, Integer idRegistro, String detalle) {
        Auditoria registro = new Auditoria();
        // Si el id no corresponde a un usuario real, el registro queda sin usuario (no se rechaza).
        Usuario usuario = (idUsuario == null) ? null : usuarioRepository.findById(idUsuario).orElse(null);
        registro.setUsuario(usuario);
        registro.setAccion(recortar(accion, 30));
        registro.setTabla(recortar(tabla, 50));
        registro.setIdRegistro(idRegistro);
        registro.setDetalle(recortar(detalle, 255));
        registro.setFecha(LocalDateTime.now());
        auditoriaRepository.save(registro);
    }

    @Override
    public void registrarDe(String username, String accion, String tabla, Integer idRegistro, String detalle) {
        registrar(idUsuarioDe(username), accion, tabla, idRegistro, detalle);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer idUsuarioDe(String username) {
        if (username == null) return null;
        return cuentaRepository.buscarPorUsername(username).map(c -> c.getUsuario().getIdUsuario()).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaResponse> listar(String accion, Integer idUsuario, LocalDate desde, LocalDate hasta, Integer limite) {
        LocalDate fin = (hasta != null) ? hasta : LocalDate.now();
        LocalDate inicio = (desde != null) ? desde : fin.minusDays(DIAS_POR_DEFECTO);
        int tamano = (limite == null) ? LIMITE_POR_DEFECTO : Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        String filtroAccion = (accion == null || accion.isBlank()) ? "%" : accion.trim().toUpperCase();

        return auditoriaRepository.buscar(
                        filtroAccion,
                        idUsuario == null ? -1 : idUsuario,
                        inicio.atStartOfDay(),
                        fin.plusDays(1).atStartOfDay(),
                        PageRequest.of(0, tamano))
                .stream()
                .map(this::aRespuesta)
                .toList();
    }

    private String recortar(String texto, int maximo) {
        if (texto == null) return null;
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    private AuditoriaResponse aRespuesta(Auditoria a) {
        Usuario u = a.getUsuario();
        return AuditoriaResponse.builder()
                .idAuditoria(a.getIdAuditoria())
                .idUsuario(u == null ? null : u.getIdUsuario())
                .usuario(u == null ? null : u.getNombres() + " " + u.getApellidos())
                .accion(a.getAccion())
                .tabla(a.getTabla())
                .idRegistro(a.getIdRegistro())
                .detalle(a.getDetalle())
                .fecha(a.getFecha())
                .build();
    }
}
