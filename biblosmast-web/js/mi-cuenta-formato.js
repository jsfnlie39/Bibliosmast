// Reglas de «Mi cuenta». El servidor ya filtra por la persona del token; aquí solo se arman los textos y listas.

const ABIERTAS = ["PENDIENTE", "LISTA"];

/**
 * Libros que la persona puede reservar: activos, con ejemplares pero ninguno disponible (si hay uno libre se pide
 * el préstamo en el mostrador) y que no tenga ya reservados. Ordenados por título.
 */
export function librosReservables(libros, reservas = []) {
    const yaReservados = new Set(reservas.filter((r) => ABIERTAS.includes(r.estado)).map((r) => r.idLibro));
    return libros
        .filter((l) => l.estado === "ACTIVO" && l.totalEjemplares > 0 && l.ejemplaresDisponibles === 0)
        .filter((l) => !yaReservados.has(l.idLibro))
        .sort((a, b) => a.titulo.localeCompare(b.titulo, "es", { sensitivity: "base" }));
}

/** Cifras de las tarjetas: préstamos sin terminar, reservas abiertas y multas por pagar (cantidad y suma). */
export function resumenCuenta({ prestamos = [], reservas = [], multas = [] }) {
    const pendientes = multas.filter((m) => m.estado === "PENDIENTE");
    return {
        prestamosActivos: prestamos.filter((p) => p.estado === "ACTIVO" || p.estado === "VENCIDO").length,
        reservasAbiertas: reservas.filter((r) => ABIERTAS.includes(r.estado)).length,
        multasPendientes: pendientes.length,
        totalMultasPendientes: pendientes.reduce((suma, m) => suma + m.valor, 0),
    };
}

/** Aviso destacado cuando hay reservas listas para recoger; "" si no hay. */
export function avisoReservasListas(reservas) {
    const listas = reservas.filter((r) => r.estado === "LISTA");
    if (listas.length === 0) return "";
    const titulos = listas.map((r) => r.titulo).join(", ");
    return listas.length === 1
        ? `Tienes un libro listo para recoger en la biblioteca: ${titulos}.`
        : `Tienes ${listas.length} libros listos para recoger en la biblioteca: ${titulos}.`;
}
