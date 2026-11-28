import { api, textoError } from "./api.js";
import { requireSesion } from "./auth.js";
import { el, vaciar, mostrarMensaje, llenarSelect } from "./dom.js";
import { formatoMoneda } from "./multas-formato.js";
import { etiquetaEstadoReserva, claseEstadoReserva, textoTurno } from "./reservas-formato.js";
import { avisoReservasListas, librosReservables, resumenCuenta } from "./mi-cuenta-formato.js";

const $ = (id) => document.getElementById(id);
const MOTIVOS = { RETRASO: "Retraso", DANO: "Daño", PERDIDA: "Pérdida" };

/** "2026-10-22" -> "22/10/2026" (sin pasar por Date, para evitar corrimientos de zona horaria). */
function formatearFecha(texto) {
    if (!texto) return "—";
    const [anio, mes, dia] = texto.split("-");
    return `${dia}/${mes}/${anio}`;
}

const formatearFechaHora = (iso) => new Date(iso).toLocaleString("es-CO", { dateStyle: "short", timeStyle: "short" });

function etiquetaPrestamo(p) {
    if (p.estado === "VENCIDO") return `Vencido (${p.diasRetraso} d)`;
    return p.estado === "FINALIZADO" ? "Finalizado" : "Activo";
}

function filaVacia(columnas, texto) {
    return el("tr", {}, el("td", { colspan: columnas, class: "vacio" }, texto));
}

function pintarPrestamos(prestamos) {
    const cuerpo = $("tbodyPrestamos");
    vaciar(cuerpo);
    if (prestamos.length === 0) return cuerpo.append(filaVacia(5, "Todavía no tienes préstamos"));
    for (const p of prestamos) {
        cuerpo.append(el("tr", {},
            el("td", {}, formatearFechaHora(p.fechaPrestamo)),
            el("td", {}, formatearFecha(p.fechaLimite)),
            el("td", {}, etiquetaPrestamo(p)),
            el("td", {}, p.renovaciones),
            el("td", {}, p.detalles.map((d) => el("div", { class: "detalle-item" },
                el("span", {}, `${d.codigo} · ${d.titulo}`),
                el("small", {}, d.fechaDevolucion ? "— devuelto" : "— sin devolver"),
            ))),
        ));
    }
}

function pintarReservas(reservas) {
    const cuerpo = $("tbodyReservas");
    vaciar(cuerpo);
    if (reservas.length === 0) return cuerpo.append(filaVacia(6, "No tienes reservas"));
    for (const r of reservas) {
        const cancelar = el("button", {
            class: "btn peligro chico", type: "button", disabled: !r.editable,
            onclick: () => cancelarReserva(r, cancelar),
        }, "Cancelar");
        cuerpo.append(el("tr", {},
            el("td", {}, r.titulo),
            el("td", {}, formatearFechaHora(r.fechaReserva)),
            el("td", {}, formatearFecha(r.fechaVencimiento)),
            el("td", {}, el("span", { class: `etiqueta ${claseEstadoReserva(r.estado)}` }, etiquetaEstadoReserva(r.estado))),
            el("td", {}, textoTurno(r)),
            el("td", { class: "acciones" }, cancelar),
        ));
    }
}

function pintarMultas(multas) {
    const cuerpo = $("tbodyMultas");
    vaciar(cuerpo);
    if (multas.length === 0) return cuerpo.append(filaVacia(4, "No tienes multas"));
    for (const m of multas) {
        cuerpo.append(el("tr", {},
            el("td", {}, formatearFechaHora(m.fecha)),
            el("td", {}, MOTIVOS[m.motivo] ?? m.motivo),
            el("td", {}, formatoMoneda.format(m.valor)),
            el("td", {}, m.estado === "PAGADA" ? "Pagada" : "Pendiente"),
        ));
    }
}

function pintarResumen(datos) {
    const r = resumenCuenta(datos);
    $("resPrestamos").textContent = r.prestamosActivos;
    $("resReservas").textContent = r.reservasAbiertas;
    $("resMultas").textContent = r.multasPendientes === 0
        ? "Ninguna"
        : `${r.multasPendientes} (${formatoMoneda.format(r.totalMultasPendientes)})`;
    mostrarMensaje($("avisoListas"), avisoReservasListas(datos.reservas), "ok");
}

async function cargar() {
    try {
        const [prestamos, reservas, multas, libros] = await Promise.all([
            api.get("/mi-cuenta/prestamos"),
            api.get("/mi-cuenta/reservas"),
            api.get("/mi-cuenta/multas"),
            api.get("/libros"),
        ]);
        pintarPrestamos(prestamos);
        pintarReservas(reservas);
        pintarMultas(multas);
        pintarResumen({ prestamos, reservas, multas });
        llenarSelect($("idLibro"),
            librosReservables(libros, reservas).map((l) => ({ id: l.idLibro, nombre: l.titulo })),
            "Libro que quieres reservar...");
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

async function reservar(evento) {
    evento.preventDefault();
    const idLibro = Number($("idLibro").value);
    if (!idLibro) return mostrarMensaje($("mensaje"), "Selecciona el libro que quieres reservar");
    $("btnReservar").disabled = true;
    try {
        const reserva = await api.post("/mi-cuenta/reservas", { idLibro });
        mostrarMensaje($("mensaje"), `Reserva registrada: quedaste en el turno ${reserva.posicionCola ?? 1} de «${reserva.titulo}»`, "ok");
        await cargar();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    } finally {
        $("btnReservar").disabled = false;
    }
}

async function cancelarReserva(reserva, boton) {
    if (!window.confirm(`¿Cancelar tu reserva de "${reserva.titulo}"?`)) return;
    boton.disabled = true;
    try {
        await api.put(`/mi-cuenta/reservas/${reserva.idReserva}/cancelar`);
        mostrarMensaje($("mensaje"), "Reserva cancelada", "ok");
        await cargar();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
        boton.disabled = false;
    }
}

async function iniciar() {
    if (!requireSesion()) return;
    $("frmReservar").addEventListener("submit", reservar);
    await cargar();
}

iniciar();
