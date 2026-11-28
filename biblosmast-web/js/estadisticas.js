// Cálculos para el panel de inicio. Son funciones puras (sin DOM ni red) para poder probarlas.

/** Cuenta cuántos elementos hay por cada valor de `clave` (nombre de campo o función). */
export function contar(items, clave) {
    const conteo = {};
    for (const item of items) {
        const valor = typeof clave === "function" ? clave(item) : item[clave];
        conteo[valor] = (conteo[valor] ?? 0) + 1;
    }
    return conteo;
}

const ETIQUETAS_EJEMPLAR = {
    DISPONIBLE: "Disponible", PRESTADO: "Prestado", RESERVADO: "Reservado", DANADO: "Dañado", PERDIDO: "Perdido",
};
const ETIQUETAS_PRESTAMO = { ACTIVO: "Activos", VENCIDO: "Vencidos", FINALIZADO: "Finalizados" };

function enOrden(conteo, etiquetas) {
    const claves = Object.keys(etiquetas).filter((k) => conteo[k] > 0);
    return { etiquetas: claves.map((k) => etiquetas[k]), valores: claves.map((k) => conteo[k]), claves };
}

export function ejemplaresPorEstado(ejemplares) {
    return enOrden(contar(ejemplares, "estado"), ETIQUETAS_EJEMPLAR);
}

export function prestamosPorEstado(prestamos) {
    return enOrden(contar(prestamos, "estado"), ETIQUETAS_PRESTAMO);
}

/** Libros por categoría, de mayor a menor. Las categorías que sobran se agrupan en «Otras». */
export function librosPorCategoria(libros, maximo = 6) {
    const orden = Object.entries(contar(libros, "categoria")).sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], "es"));
    const principales = orden.slice(0, maximo);
    const resto = orden.slice(maximo).reduce((suma, [, n]) => suma + n, 0);
    if (resto > 0) principales.push(["Otras", resto]);
    return { etiquetas: principales.map(([c]) => c), valores: principales.map(([, n]) => n) };
}

const aTexto = (fecha) => fecha.toLocaleDateString("en-CA"); // YYYY-MM-DD en hora local

/** Préstamos registrados por día en los últimos `dias` días (el último es hoy). */
export function prestamosPorDia(prestamos, dias = 14, hoy = new Date()) {
    const fechas = [];
    for (let i = dias - 1; i >= 0; i--) {
        const dia = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() - i);
        fechas.push(aTexto(dia));
    }
    const conteo = contar(prestamos, (p) => String(p.fechaPrestamo).slice(0, 10));
    return {
        etiquetas: fechas.map((f) => `${f.slice(8, 10)}/${f.slice(5, 7)}`),
        valores: fechas.map((f) => conteo[f] ?? 0),
    };
}

/** Indicadores del panel. Un dato que el usuario no puede consultar (rol) queda en null. */
export function resumen({ libros, ejemplares, prestamos, reservas, usuarios }) {
    const tiene = (x) => Array.isArray(x);
    const ejemplaresPorEst = tiene(ejemplares) ? contar(ejemplares, "estado") : null;
    const prestamosPorEst = tiene(prestamos) ? contar(prestamos, "estado") : null;
    const reservasPorEst = tiene(reservas) ? contar(reservas, "estado") : null;
    return {
        libros: tiene(libros) ? libros.length : null,
        ejemplaresTotal: tiene(ejemplares) ? ejemplares.length : null,
        ejemplaresDisponibles: ejemplaresPorEst ? (ejemplaresPorEst.DISPONIBLE ?? 0) : null,
        prestamosActivos: prestamosPorEst ? (prestamosPorEst.ACTIVO ?? 0) : null,
        prestamosVencidos: prestamosPorEst ? (prestamosPorEst.VENCIDO ?? 0) : null,
        // «Pendientes» = las que siguen abiertas: en cola o listas para recoger
        reservasPendientes: reservasPorEst ? (reservasPorEst.PENDIENTE ?? 0) + (reservasPorEst.LISTA ?? 0) : null,
        usuarios: tiene(usuarios) ? usuarios.length : null,
    };
}
