// Textos de las reglas de cada tipo de usuario (el servidor es quien las aplica).

const plural = (n, uno, varios) => `${n} ${n === 1 ? uno : varios}`;

/** "3 libros · 7 días · 2 renovaciones". Con 0 renovaciones dice "sin renovaciones". */
export function resumenTipo(tipo) {
    if (!tipo) return "—";
    const renovaciones = tipo.maxRenovaciones === 0 ? "sin renovaciones" : plural(tipo.maxRenovaciones, "renovación", "renovaciones");
    return `${plural(tipo.maxLibros, "libro", "libros")} · ${plural(tipo.diasPrestamo, "día", "días")} · ${renovaciones}`;
}

/** Cuerpo para crear o editar un tipo a partir de los textos del formulario (números enteros o null si está vacío). */
export function cuerpoTipo({ nombre, maxLibros, diasPrestamo, maxRenovaciones }) {
    const entero = (valor) => (String(valor ?? "").trim() === "" ? null : Number(valor));
    return {
        nombre: String(nombre ?? "").trim(),
        maxLibros: entero(maxLibros),
        diasPrestamo: entero(diasPrestamo),
        maxRenovaciones: entero(maxRenovaciones),
    };
}

/** Texto de la columna «Usuarios» y si el tipo se puede eliminar (sin usuarios y distinto de EXTERNO, el del registro). */
export function usoDeTipo(tipo) {
    const cantidad = tipo.cantidadUsuarios ?? 0;
    return {
        texto: plural(cantidad, "usuario", "usuarios"),
        eliminable: cantidad === 0 && tipo.nombre !== "EXTERNO",
    };
}
