import { requireSesion, logout } from "./auth.js";
import { cargarPanel } from "./panel.js";
import { decodificarJwt } from "./sesion.js";

/** Muestra el token de la sesion: su texto, su contenido decodificado y hasta cuando vale. */
function mostrarToken(sesion) {
    const { header, payload } = decodificarJwt(sesion.token);
    const vence = new Date(sesion.expiraEn * 1000).toLocaleTimeString("es-CO", { timeStyle: "short" });
    document.getElementById("tokenVigencia").textContent = `Tu sesión es válida hasta las ${vence}.`;
    document.getElementById("tokenTexto").textContent = sesion.token;
    document.getElementById("tokenHeader").textContent = JSON.stringify(header, null, 2);
    document.getElementById("tokenPayload").textContent = JSON.stringify(payload, null, 2);
}

function iniciar() {
    const sesion = requireSesion();
    if (!sesion) return;
    document.getElementById("usuarioActual").textContent =
        `${sesion.nombres} ${sesion.apellidos} · ${sesion.rol}`;
    document.getElementById("btnSalir").addEventListener("click", logout);
    mostrarToken(sesion);
    cargarPanel(sesion).catch(() => {}); // indicadores y graficos; si fallan, el resto de la pagina sigue funcionando

    // Catalogos, ejemplares, prestamos, reservas y multas: solo para el personal de la biblioteca
    if (!["ADMIN", "BIBLIOTECARIO"].includes(sesion.rol)) {
        for (const id of ["modCatalogos", "modEjemplares", "modPrestamos", "modReservas", "modMultas"]) {
            const modulo = document.getElementById(id);
            modulo.classList.add("deshabilitado");
            modulo.removeAttribute("href");
            modulo.querySelector("p").textContent = "Solo para el personal de la biblioteca.";
        }
    }

    // Usuarios y auditoria: solo para el administrador
    if (sesion.rol !== "ADMIN") {
        for (const id of ["modUsuarios", "modAuditoria"]) {
            const modulo = document.getElementById(id);
            modulo.classList.add("deshabilitado");
            modulo.removeAttribute("href");
            modulo.querySelector("p").textContent = "Solo para el administrador.";
        }
    }
}

iniciar();
