// Textos y reglas de presentación del portal «Mi cuenta» (el servidor valida todo de verdad).
import { formatoMoneda } from "./multas-formato.js";

const MOTIVOS = { RETRASO: "Retraso", DANO: "Daño", PERDIDA: "Pérdida" };

/** "2026-10-22" -> "22/10/2026" (sin pasar por Date, para evitar corrimientos de zona horaria). */
export function formatearFecha(texto) {
    if (!texto) return "—";
    const [anio, mes, dia] = texto.split("-");
    return `${dia}/${mes}/${anio}`;
}

export function textoMotivoMulta(motivo) {
    return MOTIVOS[motivo] ?? motivo;
}

/** Se puede cancelar una reserva pendiente o lista para recoger (las demás ya terminaron). */
export function puedeCancelarReserva(reserva) {
    return reserva?.estado === "PENDIENTE" || reserva?.estado === "LISTA";
}

/** Qué le toca hacer a la persona con esta reserva. */
export function textoSituacionReserva(reserva) {
    if (reserva?.estado === "LISTA") {
        const ejemplar = reserva.codigoEjemplar ? ` (ejemplar ${reserva.codigoEjemplar})` : "";
        return `Ya puedes recogerlo en la biblioteca${ejemplar} hasta el ${formatearFecha(reserva.fechaLimiteRecogida)}.`;
    }
    if (reserva?.estado === "PENDIENTE") {
        const puesto = reserva.posicionCola ? `Vas en el lugar ${reserva.posicionCola} de la cola. ` : "";
        return `${puesto}Vence el ${formatearFecha(reserva.fechaVencimiento)}.`;
    }
    return "";
}

/** Libros que vale la pena reservar: tienen ejemplares pero ninguno está disponible. */
export function librosReservables(libros) {
    return (libros ?? []).filter((l) => l.estado === "ACTIVO" && l.totalEjemplares > 0 && l.ejemplaresDisponibles === 0);
}

/** Frase sobre las multas pendientes; "" si no debe nada. */
export function textoMultasPendientes(total) {
    const valor = Number(total ?? 0);
    if (valor <= 0) return "";
    return `Tienes multas pendientes por ${formatoMoneda.format(valor)}. Debes pagarlas en la biblioteca antes de recibir un nuevo préstamo.`;
}
