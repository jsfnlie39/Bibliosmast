import { api } from "./api.js";
import { montarLayout } from "./layout.js";
import { borrarSesion, getSesion, guardarSesion, tokenVencido } from "./sesion.js";

export { getSesion };

/** Envia usuario y clave; si son correctos el servidor responde con los datos del usuario y su token JWT. */
export async function login(username, password) {
    const sesion = await api.post("/auth/login", { username, password });
    guardarSesion(sesion);
    return sesion;
}

/** Avisa al servidor (para que el token deje de servir) y limpia la sesion del navegador. */
export async function logout() {
    try {
        if (getSesion()?.token) await api.post("/auth/logout", {});
    } catch {
        /* sin conexion o token ya invalido: de todos modos se cierra la sesion aqui */
    }
    borrarSesion();
    window.location.replace("index.html");
}

/** Para las paginas internas: sin sesion, o con el token vencido, vuelve al login. */
export function requireSesion() {
    const sesion = getSesion();
    if (!sesion) {
        window.location.replace("index.html");
        return null;
    }
    if (tokenVencido(sesion)) {
        borrarSesion();
        window.location.replace("index.html?motivo=expirada");
        return null;
    }
    // Con una clave temporal solo se puede entrar a "Mi perfil" a cambiarla
    if (sesion.debeCambiarClave && document.body.dataset.pagina !== "perfil") {
        window.location.replace("perfil.html?cambiar=1");
        return null;
    }
    montarLayout(sesion); // menu lateral, barra superior, tema y tiempo de sesion
    return sesion;
}
