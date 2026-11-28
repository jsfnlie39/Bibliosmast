// Sesion guardada en el navegador (sessionStorage): datos del usuario + token JWT.
// Lo usan auth.js y api.js; api.js no importa auth.js para evitar una dependencia circular.
export const CLAVE_SESION = "biblosmast.sesion";

export function getSesion() {
    try {
        return JSON.parse(sessionStorage.getItem(CLAVE_SESION));
    } catch {
        return null;
    }
}

export function guardarSesion(sesion) {
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
}

export function borrarSesion() {
    sessionStorage.removeItem(CLAVE_SESION);
}

/** True si la sesion no tiene token o el token ya vencio (expiraEn viene en segundos desde 1970). */
export function tokenVencido(sesion) {
    return !sesion?.token || Date.now() / 1000 >= sesion.expiraEn;
}

/**
 * Lee el contenido de un JWT (header y payload) SIN verificar la firma.
 * Solo sirve para mostrarlo: la firma la verifica el servidor.
 */
export function decodificarJwt(token) {
    const [cabecera, contenido] = token.split(".");
    const aJson = (parte) => {
        const base64 = parte.replace(/-/g, "+").replace(/_/g, "/");
        const bytes = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0));
        return JSON.parse(new TextDecoder().decode(bytes));
    };
    return { header: aJson(cabecera), payload: aJson(contenido) };
}

/** True si la sesion es de un administrador (solo para mostrar u ocultar botones; el servidor lo vuelve a comprobar). */
export function esAdmin() {
    return getSesion()?.rol === "ADMIN";
}
