import { api, textoError } from "./api.js";
import { requireSesion } from "./auth.js";
import { el, vaciar, mostrarMensaje } from "./dom.js";
import { textoMovimiento } from "./ficha-formato.js";
import { etiquetaEstadoReserva, claseEstadoReserva, textoTurno } from "./reservas-formato.js";

const $ = (id) => document.getElementById(id);
const ROLES_PERSONAL = ["ADMIN", "BIBLIOTECARIO"];

/** "2026-10-22" -> "22/10/2026" (sin pasar por Date, para evitar corrimientos de zona horaria). */
function formatearFecha(texto) {
    if (!texto) return "—";
    const [anio, mes, dia] = texto.slice(0, 10).split("-");
    return `${dia}/${mes}/${anio}`;
}

const formatearFechaHora = (iso) => (iso ? new Date(iso).toLocaleString("es-CO", { dateStyle: "short", timeStyle: "short" }) : "—");

function filaVacia(columnas, texto) {
    return el("tr", {}, el("td", { colspan: columnas, class: "vacio" }, texto));
}

function pintar(ficha) {
    const l = ficha.libro;
    document.title = `Biblosmast | ${l.titulo}`;
    $("titulo").textContent = l.titulo;
    $("dIsbn").textContent = l.isbn;
    $("dAutores").textContent = l.autores.length ? l.autores.join(", ") : "—";
    $("dCategoria").textContent = l.categoria;
    $("dEditorial").textContent = l.editorial;
    $("dAnio").textContent = `${l.anioPublicacion ?? "—"} · ${l.idioma ?? "—"}`;
    $("dEstado").textContent = l.estado;
    $("dEjemplares").textContent = `${l.ejemplaresDisponibles} disponibles de ${l.totalEjemplares}`;
    $("dPrestamos").textContent = `${ficha.totalPrestamos} (${ficha.prestamosSinDevolver} sin devolver)`;

    const ejemplares = $("tbodyEjemplares");
    vaciar(ejemplares);
    if (ficha.ejemplares.length === 0) ejemplares.append(filaVacia(4, "Este libro no tiene ejemplares"));
    for (const e of ficha.ejemplares) {
        ejemplares.append(el("tr", {},
            el("td", {}, e.codigo), el("td", {}, e.ubicacion ?? "—"), el("td", {}, e.estado), el("td", {}, formatearFecha(e.fechaIngreso))));
    }

    const prestamos = $("tbodyPrestamos");
    vaciar(prestamos);
    if (ficha.prestamos.length === 0) prestamos.append(filaVacia(6, "Todavía no se ha prestado"));
    for (const m of ficha.prestamos) {
        prestamos.append(el("tr", {},
            el("td", {}, formatearFechaHora(m.fechaPrestamo)),
            el("td", {}, el("a", { class: "ficha-vinculo", href: `ficha-usuario.html?id=${m.idUsuario}` }, `${m.usuario} (${m.documento})`)),
            el("td", {}, m.codigoEjemplar),
            el("td", {}, formatearFecha(m.fechaLimite)),
            el("td", {}, formatearFechaHora(m.fechaDevolucion)),
            el("td", {}, textoMovimiento(m)),
        ));
    }

    const reservas = $("tbodyReservas");
    vaciar(reservas);
    if (ficha.reservas.length === 0) reservas.append(filaVacia(4, "No hay reservas de este libro"));
    for (const r of ficha.reservas) {
        reservas.append(el("tr", {},
            el("td", {}, `${r.usuario} (${r.documento})`),
            el("td", {}, formatearFechaHora(r.fechaReserva)),
            el("td", {}, el("span", { class: `etiqueta ${claseEstadoReserva(r.estado)}` }, etiquetaEstadoReserva(r.estado))),
            el("td", {}, textoTurno(r)),
        ));
    }
}

async function iniciar() {
    const sesion = requireSesion();
    if (!sesion) return;
    // La ficha muestra datos de otras personas: solo el personal de la biblioteca
    if (!ROLES_PERSONAL.includes(sesion.rol)) {
        window.location.replace("libros.html");
        return;
    }
    const id = Number(new URLSearchParams(window.location.search).get("id"));
    if (!Number.isInteger(id) || id <= 0) {
        mostrarMensaje($("mensaje"), "Falta indicar el libro: abre la ficha desde la lista de libros");
        return;
    }
    try {
        pintar(await api.get(`/libros/${id}/historial`));
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

iniciar();
