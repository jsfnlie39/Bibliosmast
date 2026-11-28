// Textos de la campana de avisos. El servidor decide qué avisos existen y si están leídos.

const ETIQUETAS = { POR_VENCER: "Por vencer", VENCIDO: "Vencido", RESERVA_LISTA: "Reserva lista" };

/** Número de la insignia: vacío si no hay avisos sin leer, y «9+» desde el décimo. */
export function textoInsignia(cantidad) {
    if (!cantidad || cantidad < 1) return "";
    return cantidad > 9 ? "9+" : String(cantidad);
}

export function etiquetaTipoAviso(tipo) {
    return ETIQUETAS[tipo] ?? tipo ?? "Aviso";
}

/** «hace 5 min», «hace 3 h», «ayer» o la fecha (dd/mm/aaaa). `ahora` se puede indicar para probarlo. */
export function tiempoRelativo(iso, ahora = new Date()) {
    const fecha = new Date(iso);
    const minutos = Math.floor((ahora.getTime() - fecha.getTime()) / 60000);
    if (Number.isNaN(minutos) || minutos < 0) return "";
    if (minutos < 1) return "ahora";
    if (minutos < 60) return `hace ${minutos} min`;
    const horas = Math.floor(minutos / 60);
    if (horas < 24) return `hace ${horas} h`;
    const dias = Math.floor(horas / 24);
    if (dias === 1) return "ayer";
    return fecha.toLocaleDateString("es-CO");
}

/** Descripción para lectores de pantalla del botón de la campana. */
export function descripcionCampana(cantidad) {
    if (!cantidad || cantidad < 1) return "Avisos: no tienes avisos sin leer";
    return cantidad === 1 ? "Avisos: tienes 1 aviso sin leer" : `Avisos: tienes ${cantidad} avisos sin leer`;
}
