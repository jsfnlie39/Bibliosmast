// Datos de los reportes listos para tablas y gráficos. Los números los calcula el servidor.

const MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];

/** "2026-11" -> "nov 2026". Si el texto no tiene ese formato se devuelve tal cual. */
export function etiquetaMes(mes) {
    const partes = /^(\d{4})-(\d{2})$/.exec(mes ?? "");
    if (!partes) return mes ?? "";
    return `${MESES[Number(partes[2]) - 1] ?? partes[2]} ${partes[1]}`;
}

/** Texto de consulta con el periodo y el límite; omite lo que esté vacío. */
export function parametrosReporte({ desde, hasta, limite }) {
    const params = new URLSearchParams();
    if (desde) params.set("desde", desde);
    if (hasta) params.set("hasta", hasta);
    if (limite) params.set("limite", String(limite));
    const texto = params.toString();
    return texto ? `?${texto}` : "";
}

/** Etiquetas y valores del gráfico de préstamos por mes. */
export function serieMeses(items) {
    return { etiquetas: items.map((i) => etiquetaMes(i.mes)), valores: items.map((i) => i.prestamos) };
}

/** Etiquetas y valores del gráfico de libros más prestados (los títulos largos se acortan). */
export function serieLibros(items, largo = 28) {
    const corto = (t) => (t.length > largo ? `${t.slice(0, largo - 1)}…` : t);
    return { etiquetas: items.map((i) => corto(i.titulo)), valores: items.map((i) => i.prestamos) };
}

/** Dos series para el gráfico de retrasos: multas por retraso en el periodo y ejemplares vencidos ahora. */
export function serieRetrasos(items, largo = 24) {
    const corto = (t) => (t.length > largo ? `${t.slice(0, largo - 1)}…` : t);
    return {
        etiquetas: items.map((i) => corto(i.usuario)),
        multas: items.map((i) => i.multasRetraso),
        vencidos: items.map((i) => i.ejemplaresVencidosAhora),
    };
}
