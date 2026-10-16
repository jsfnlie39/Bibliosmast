import { API_URL } from "./config.js";

export class ApiError extends Error {
    constructor(status, mensaje, errores = null) {
        super(mensaje);
        this.status = status;
        this.errores = errores;
    }
}

async function request(ruta, { method = "GET", body } = {}) {
    const opciones = { method, headers: { Accept: "application/json" } };
    if (body !== undefined) {
        opciones.headers["Content-Type"] = "application/json";
        opciones.body = JSON.stringify(body);
    }

    let respuesta;
    try {
        respuesta = await fetch(`${API_URL}${ruta}`, opciones);
    } catch {
        throw new ApiError(0, "No se pudo conectar con el servidor. Verifica que la API esté encendida.");
    }

    if (respuesta.status === 204) return null;

    let datos = null;
    try {
        datos = await respuesta.json();
    } catch {
        /* respuesta sin cuerpo JSON */
    }

    if (!respuesta.ok) {
        throw new ApiError(respuesta.status, datos?.mensaje ?? "Error inesperado", datos?.errores ?? null);
    }
    return datos;
}

export const api = {
    get: (ruta) => request(ruta),
    post: (ruta, body) => request(ruta, { method: "POST", body }),
    put: (ruta, body) => request(ruta, { method: "PUT", body }),
    delete: (ruta) => request(ruta, { method: "DELETE" }),
};

/** Convierte un ApiError en un texto para mostrar al usuario (incluye errores por campo). */
export function textoError(error) {
    if (!(error instanceof ApiError)) return "Ocurrió un error inesperado";
    const detalles = error.errores ? Object.values(error.errores).join(". ") : "";
    return detalles ? `${error.message}: ${detalles}` : error.message;
}
