package com.biblosmast.service;

import com.biblosmast.model.dto.MultaResponse;
import com.biblosmast.model.entity.EstadoMulta;
import com.biblosmast.model.entity.Multa;
import com.biblosmast.model.entity.Usuario;
import com.biblosmast.repository.MultaRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MultaService implements IMultaService {

    private final MultaRepository multaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MultaResponse> listar(String estado, Integer idUsuario) {
        String filtro = (estado == null) ? "" : estado.trim().toUpperCase();
        return multaRepository.listarTodas().stream()
                .map(this::aRespuesta)
                .filter(m -> filtro.isEmpty() || m.getEstado().equals(filtro))
                .filter(m -> idUsuario == null || m.getIdUsuario().equals(idUsuario))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MultaResponse obtener(Integer id) {
        return aRespuesta(buscarOFalla(id));
    }

    @Override
    @Transactional
    public MultaResponse pagar(Integer id) {
        Multa multa = buscarOFalla(id);
        if (multa.getEstado() == EstadoMulta.PAGADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta multa ya fue pagada");
        }
        multa.setEstado(EstadoMulta.PAGADA);
        multa.setFechaPago(LocalDateTime.now());
        return aRespuesta(multaRepository.save(multa));
    }

    @Override
    @Transactional
    public void anular(Integer id) {
        Multa multa = buscarOFalla(id);
        if (multa.getEstado() == EstadoMulta.PAGADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede anular una multa que ya fue pagada");
        }
        multaRepository.delete(multa);
    }

    private Multa buscarOFalla(Integer id) {
        return multaRepository.buscarConDetalle(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Multa no encontrada"));
    }

    private MultaResponse aRespuesta(Multa m) {
        Usuario u = m.getUsuario();
        return MultaResponse.builder()
                .idMulta(m.getIdMulta())
                .idPrestamo(m.getPrestamo().getIdPrestamo())
                .idUsuario(u.getIdUsuario())
                .usuario(u.getNombres() + " " + u.getApellidos())
                .documento(u.getDocumento())
                .valor(m.getValor())
                .motivo(m.getMotivo().name())
                .estado(m.getEstado().name())
                .fecha(m.getFecha())
                .fechaPago(m.getFechaPago())
                .build();
    }
}
