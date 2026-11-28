// Documento simulado (jsdom) para probar los módulos que usan el DOM.
import { JSDOM } from "jsdom";

export function crearDom(html = "<!DOCTYPE html><html><body></body></html>") {
    const dom = new JSDOM(html, { url: "http://localhost:5173/menu.html", pretendToBeVisual: true });
    const { window } = dom;
    for (const nombre of ["window", "document", "MutationObserver", "CustomEvent", "Event", "HTMLElement", "Node", "localStorage", "sessionStorage", "getComputedStyle"]) {
        Object.defineProperty(globalThis, nombre, { value: nombre === "window" ? window : typeof window[nombre] === "function" ? window[nombre].bind(window) : window[nombre], configurable: true, writable: true });
    }
    // En las pruebas no queremos temporizadores que mantengan vivo el proceso
    globalThis.setInterval = () => 0;
    return dom;
}

/** Espera a que se ejecuten los MutationObserver (se disparan al terminar la tarea actual). */
export const esperar = () => new Promise((r) => setTimeout(r, 0));

/**
 * jsdom no deja redefinir window.location. Para probar páginas que redirigen o leen la URL,
 * se reemplaza el `window` global por un envoltorio que devuelve una ubicación simulada.
 */
export function simularUbicacion({ search = "", replace = () => {} } = {}) {
    const real = globalThis.window;
    const ubicacion = { search, replace };
    globalThis.window = new Proxy(real, {
        get(objetivo, propiedad) {
            if (propiedad === "location") return ubicacion;
            const valor = Reflect.get(objetivo, propiedad);
            return typeof valor === "function" ? valor.bind(objetivo) : valor;
        },
    });
    return { ubicacion, real };
}
