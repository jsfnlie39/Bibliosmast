// Reglas de presentación de la renovación de préstamos (el servidor valida todas las reglas de verdad).

/** Solo se ofrece «Renovar» en préstamos activos: los vencidos y finalizados no se renuevan. */
export function puedeRenovar(prestamo) {
    return prestamo?.estado === "ACTIVO";
}

/** "Sin renovar", "1 vez", "2 veces". */
export function textoRenovaciones(cantidad) {
    const n = cantidad ?? 0;
    return n === 0 ? "Sin renovar" : n === 1 ? "1 vez" : `${n} veces`;
}

/** Mensaje al renovar con éxito; la fecha llega como "YYYY-MM-DD". */
export function textoRenovado(prestamo) {
    const [anio, mes, dia] = prestamo.fechaLimite.split("-");
    return `Préstamo N° ${prestamo.idPrestamo} renovado. Nueva fecha límite: ${dia}/${mes}/${anio}.`;
}
