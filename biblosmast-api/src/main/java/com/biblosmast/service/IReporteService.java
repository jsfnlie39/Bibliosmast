package com.biblosmast.service;

import com.biblosmast.model.dto.LibroPrestadoItem;
import com.biblosmast.model.dto.PrestamosMesItem;
import com.biblosmast.model.dto.UsuarioRetrasoItem;
import java.time.LocalDate;
import java.util.List;

/** Reportes para el personal. Las fechas son opcionales: por defecto, los últimos 12 meses hasta hoy. */
public interface IReporteService {

    List<LibroPrestadoItem> librosMasPrestados(LocalDate desde, LocalDate hasta, int limite);

    /** Usuarios con más multas por retraso en el periodo, junto con lo que tienen vencido ahora mismo. */
    List<UsuarioRetrasoItem> usuariosConRetrasos(LocalDate desde, LocalDate hasta, int limite);

    /** Préstamos por mes de los últimos N meses (el mes actual incluido), con ceros en los meses sin movimiento. */
    List<PrestamosMesItem> prestamosPorMes(int meses);
}
