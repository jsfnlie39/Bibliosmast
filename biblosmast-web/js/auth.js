import { api } from "./api.js";

const CLAVE_SESION = "biblosmast.sesion";

export async function login(username, password) {
    const usuario = await api.post("/auth/login", { username, password });
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify(usuario));
    return usuario;
}

export function getSesion() {
    try {
        return JSON.parse(sessionStorage.getItem(CLAVE_SESION));
    } catch {
        return null;
    }
}

export function logout() {
    sessionStorage.removeItem(CLAVE_SESION);
    window.location.replace("index.html");
}

/** Para las paginas internas: si no hay sesion, vuelve al login. */
export function requireSesion() {
    const sesion = getSesion();
    if (!sesion) {
        window.location.replace("index.html");
        return null;
    }
    return sesion;
}
