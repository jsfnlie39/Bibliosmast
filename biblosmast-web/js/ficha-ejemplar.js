import { api, textoError } from "./api.js";
import { requireSesion } from "./auth.js";
import { el, vaciar, mostrarMensaje } from "./dom.js";
import { resumenCondicion, textoMovimiento } from "./ficha-formato.js";

const $ = (id) => document.getElementById(id);
const ROLES_PERSONAL = ["ADMIN", "BIBLIOTECARIO"];

/** "2026-10-22" -> "22/10/2026" (sin pasar por Date, para evitar corrimientos de zona horaria). */
function formatearFecha(texto) {
    if (!texto) return "—";
    const [anio, mes, dia] = texto.slice(0, 10).split("-");
    return `${dia}/${mes}/${anio}`;
}

const formatearFechaHora = (iso) => (iso ? new Date(iso).toLocaleString("es-CO", { dateStyle: "short", timeStyle: "short" }) : "—");

function pintar(ficha) {
    const e = ficha.ejemplar;
    document.title = `Biblosmast | ${e.codigo}`;
    $("titulo").textContent = `Ejemplar ${e.codigo}`;
    $("dLibro").replaceChildren(el("a", { class: "ficha-vinculo", href: `ficha-libro.html?id=${e.idLibro}` }, e.titulo));
    $("dUbicacion").textContent = e.ubicacion ?? "—";
    $("dEstado").textContent = e.estado;
    $("dIngreso").textContent = formatearFecha(e.fechaIngreso);
    $("dPrestamos").textContent = `${ficha.totalPrestamos} (${ficha.prestamosSinDevolver} sin devolver)`;
    $("dCondicion").textContent = resumenCondicion(ficha);

    const cuerpo = $("tbodyPrestamos");
    vaciar(cuerpo);
    if (ficha.prestamos.length === 0) {
        cuerpo.append(el("tr", {}, el("td", { colspan: 6, class: "vacio" }, "Esta copia todavía no se ha prestado")));
        return;
    }
    for (const m of ficha.prestamos) {
        cuerpo.append(el("tr", {},
            el("td", {}, formatearFechaHora(m.fechaPrestamo)),
            el("td", {}, el("a", { class: "ficha-vinculo", href: `ficha-usuario.html?id=${m.idUsuario}` }, `${m.usuario} (${m.documento})`)),
            el("td", {}, formatearFecha(m.fechaLimite)),
            el("td", {}, formatearFechaHora(m.fechaDevolucion)),
            el("td", {}, textoMovimiento(m)),
            el("td", {}, m.observacionDevolucion ?? "—"),
        ));
    }
}

async function iniciar() {
    const sesion = requireSesion();
    if (!sesion) return;
    // El seguimiento muestra datos de otras personas: solo el personal de la biblioteca
    if (!ROLES_PERSONAL.includes(sesion.rol)) {
        window.location.replace("libros.html");
        return;
    }
    const id = Number(new URLSearchParams(window.location.search).get("id"));
    if (!Number.isInteger(id) || id <= 0) {
        mostrarMensaje($("mensaje"), "Falta indicar el ejemplar: abre la ficha desde la lista de ejemplares");
        return;
    }
    try {
        pintar(await api.get(`/ejemplares/${id}/historial`));
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

iniciar();
