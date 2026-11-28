// Utilidades de dinero y de texto para el módulo de multas (se usan en Multas y en Préstamos).

export const formatoMoneda = new Intl.NumberFormat("es-CO", {
    style: "currency", currency: "COP", minimumFractionDigits: 0, maximumFractionDigits: 2,
});

/**
 * Texto para avisar, al devolver un ejemplar, si se generaron multas.
 * Devuelve "" cuando no hubo ninguna.
 */
export function textoMultasGeneradas(respuesta) {
    const cantidad = respuesta?.multasGeneradas ?? 0;
    if (cantidad <= 0) return "";
    const total = formatoMoneda.format(respuesta.valorMultasGeneradas ?? 0);
    return cantidad === 1
        ? ` Se generó 1 multa por ${total}.`
        : ` Se generaron ${cantidad} multas por un total de ${total}.`;
}
