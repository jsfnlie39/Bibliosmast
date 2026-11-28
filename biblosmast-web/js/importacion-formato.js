// Importar libros desde CSV: plantilla, validación del archivo y textos del resultado.
// El servidor revisa cada línea y decide; aquí solo se prepara lo que ve la persona.

const MAX_BYTES = 1_000_000;

/** Plantilla descargable. Lleva la marca BOM para que Excel respete las tildes, y «;» como separador. */
export const PLANTILLA_CSV = "\uFEFF"
    + "isbn;titulo;autores;categoria;editorial;anio;idioma;edicion;paginas;descripcion\n"
    + "978-0-00-000000-0;Título de ejemplo;Nombre Apellido|Otro Autor;Nombre de la categoría;Nombre de la editorial;2020;Español;1.ª edición;300;Descripción opcional\n";

/** "" si el archivo sirve; si no, el motivo. */
export function validarArchivo(archivo) {
    if (!archivo) return "Selecciona un archivo CSV";
    if (!/\.(csv|txt)$/i.test(archivo.name ?? "")) return "El archivo debe ser un CSV (.csv)";
    if (archivo.size === 0) return "El archivo está vacío";
    if (archivo.size > MAX_BYTES) return "El archivo es demasiado grande (máximo 1 MB)";
    return "";
}

/** Un CSV guardado en ANSI (el «CSV» normal de Excel) deja el carácter � donde había tildes. */
export function tieneProblemaDeCodificacion(texto) {
    return String(texto ?? "").includes("\uFFFD");
}

/** Solo se puede importar tras una revisión sin errores y con al menos una línea. */
export function puedeImportar(resultado) {
    return Boolean(resultado) && resultado.soloValidar === true && resultado.lineasConErrores === 0 && resultado.lineasValidas > 0;
}

const plural = (n, uno, varios) => `${n} ${n === 1 ? uno : varios}`;

/** Resumen de una revisión o de una importación. */
export function resumenImportacion(r) {
    if (!r) return "";
    if (r.importado) return `Se importaron ${plural(r.librosCreados, "libro", "libros")}.`;
    if (r.lineasConErrores > 0) {
        const base = `Revisadas ${plural(r.totalLineas, "línea", "líneas")}: ${plural(r.lineasValidas, "correcta", "correctas")} y ${plural(r.lineasConErrores, "con errores", "con errores")}.`;
        return `${base} No se guardó nada: corrige el archivo y vuelve a revisarlo.`;
    }
    return r.soloValidar
        ? `Las ${plural(r.totalLineas, "línea", "líneas")} están correctas. Pulsa «Importar» para guardarlas.`
        : "No se importó nada.";
}

/** Texto de la columna «Resultado» de cada línea. */
export function textoLinea(linea) {
    return linea.errores.length === 0 ? "Correcta" : linea.errores.join(" · ");
}
