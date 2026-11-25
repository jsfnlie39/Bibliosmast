// Reglas de presentación de la cola de reservas (el servidor decide el turno y el estado de verdad).

const ETIQUETAS = {
    PENDIENTE: "En cola",
    LISTA: "Lista para recoger",
    VENCIDA: "Vencida",
    ATENDIDA: "Atendida",
    CANCELADA: "Cancelada",
};

const CLASES = {
    PENDIENTE: "activo",
    LISTA: "lista",
    VENCIDA: "vencido",
    ATENDIDA: "finalizado",
    CANCELADA: "finalizado",
};

/** "Lista para recoger", "En cola"... Si llega un estado desconocido lo muestra tal cual. */
export function etiquetaEstadoReserva(estado) {
    return ETIQUETAS[estado] ?? estado ?? "—";
}

/** Clase CSS de la etiqueta del estado. */
export function claseEstadoReserva(estado) {
    return CLASES[estado] ?? "finalizado";
}

/** "2026-10-22" -> "22/10/2026" (sin pasar por Date, para evitar corrimientos de zona horaria). */
function fechaCorta(texto) {
    const [anio, mes, dia] = texto.split("-");
    return `${dia}/${mes}/${anio}`;
}

/** Columna «Turno / recogida»: el turno en la cola, o hasta cuándo y qué ejemplar recoger. */
export function textoTurno(reserva) {
    if (reserva?.estado === "PENDIENTE" && reserva.posicionCola) {
        return `Turno ${reserva.posicionCola}`;
    }
    if (reserva?.estado === "LISTA" && reserva.fechaLimiteRecogida) {
        const ejemplar = reserva.codigoEjemplar ? ` · ${reserva.codigoEjemplar}` : "";
        return `Recoger hasta ${fechaCorta(reserva.fechaLimiteRecogida)}${ejemplar}`;
    }
    return "—";
}

/** Estados que se pueden elegir al editar: una reserva lista solo se deja así o se cancela. */
export function opcionesEstadoEdicion(estado) {
    return estado === "LISTA" ? ["LISTA", "CANCELADA"] : ["PENDIENTE", "ATENDIDA", "CANCELADA"];
}

/** Aviso al devolver un ejemplar que quedó apartado para quien esperaba en la cola. "" si nadie esperaba. */
export function textoApartado(respuesta) {
    return respuesta?.apartadoPara
        ? ` El ejemplar quedó apartado para ${respuesta.apartadoPara}, que ya puede recogerlo.`
        : "";
}
