// Tema claro / oscuro. La preferencia se guarda en el navegador (localStorage).
const CLAVE_TEMA = "biblosmast.tema";

function leerTema() {
    try {
        return localStorage.getItem(CLAVE_TEMA);
    } catch {
        return null;
    }
}

export function temaActual() {
    return document.documentElement.dataset.tema === "oscuro" ? "oscuro" : "claro";
}

export function aplicarTema(tema) {
    document.documentElement.dataset.tema = tema;
}

/** Aplica el tema guardado; si no hay, el que prefiere el sistema operativo. */
export function iniciarTema() {
    const guardado = leerTema();
    const delSistema = window.matchMedia?.("(prefers-color-scheme: dark)").matches ? "oscuro" : "claro";
    const tema = guardado === "oscuro" || guardado === "claro" ? guardado : delSistema;
    aplicarTema(tema);
    return tema;
}

export function alternarTema() {
    const nuevo = temaActual() === "oscuro" ? "claro" : "oscuro";
    aplicarTema(nuevo);
    try {
        localStorage.setItem(CLAVE_TEMA, nuevo);
    } catch {
        /* sin almacenamiento: el tema vale solo para esta visita */
    }
    window.dispatchEvent(new CustomEvent("biblosmast:tema", { detail: nuevo }));
    return nuevo;
}
