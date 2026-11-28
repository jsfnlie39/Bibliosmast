// Búsqueda avanzada de libros y textos de las fichas. El servidor es quien filtra y calcula.

const sinTexto = (v) => v === undefined || v === null || String(v).trim() === "";

/** ¿Hay algún filtro activo? (el texto de búsqueda también cuenta) */
export function hayFiltros(f) {
    return !sinTexto(f.q) || !sinTexto(f.idAutor) || !sinTexto(f.idCategoria) || !sinTexto(f.idEditorial)
        || !sinTexto(f.idioma) || !sinTexto(f.anioDesde) || !sinTexto(f.anioHasta) || Boolean(f.soloDisponibles);
}

/** Texto de consulta (?a=1&b=2) solo con los filtros que tienen valor; "" si no hay ninguno. */
export function parametrosBusqueda(f) {
    const params = new URLSearchParams();
    for (const clave of ["q", "idAutor", "idCategoria", "idEditorial", "idioma", "anioDesde", "anioHasta"]) {
        if (!sinTexto(f[clave])) params.set(clave, String(f[clave]).trim());
    }
    if (f.soloDisponibles) params.set("soloDisponibles", "true");
    const texto = params.toString();
    return texto ? `?${texto}` : "";
}

const porNombre = (a, b) => a.nombre.localeCompare(b.nombre, "es", { sensitivity: "base" });

/** Opciones de los selectores de filtro a partir de los libros (sin repetidos, ordenadas). Sirve para cualquier rol. */
export function opcionesDeFiltro(libros) {
    const autores = new Map();
    const categorias = new Map();
    const editoriales = new Map();
    const idiomas = new Set();
    for (const libro of libros) {
        (libro.idsAutores ?? []).forEach((id, i) => autores.set(id, libro.autores[i]));
        categorias.set(libro.idCategoria, libro.categoria);
        editoriales.set(libro.idEditorial, libro.editorial);
        if (!sinTexto(libro.idioma)) idiomas.add(libro.idioma.trim());
    }
    const lista = (mapa) => [...mapa].map(([id, nombre]) => ({ id, nombre })).sort(porNombre);
    return {
        autores: lista(autores),
        categorias: lista(categorias),
        editoriales: lista(editoriales),
        idiomas: [...idiomas].map((i) => ({ id: i, nombre: i })).sort(porNombre),
    };
}

/** Estado de un ejemplar en el historial de un libro. */
export function textoMovimiento(m) {
    if (!m.fechaDevolucion) return m.vencido ? "Sin devolver (vencido)" : "Sin devolver";
    if (m.estadoDevolucion === "DANADO") return "Devuelto dañado";
    if (m.estadoDevolucion === "PERDIDO") return "Perdido";
    return "Devuelto en buen estado";
}

/** «2 de 3 libros» con el cupo que le da su tipo de usuario. */
export function textoCupo(ficha) {
    return `${ficha.ejemplaresSinDevolver} de ${ficha.maxLibros} libros`;
}
