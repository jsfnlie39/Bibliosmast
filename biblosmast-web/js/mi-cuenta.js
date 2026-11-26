import { api, textoError } from "./api.js";
import { requireSesion, logout } from "./auth.js";
import { el, vaciar, mostrarMensaje, llenarSelect } from "./dom.js";
import { formatoMoneda } from "./multas-formato.js";
import { puedeRenovar, textoRenovaciones, textoRenovado } from "./prestamos-formato.js";
import { claseEstadoReserva, etiquetaEstadoReserva } from "./reservas-formato.js";
import {
    formatearFecha, librosReservables, puedeCancelarReserva, textoMotivoMulta,
    textoMultasPendientes, textoSituacionReserva,
} from "./mi-cuenta-formato.js";

const $ = (id) => document.getElementById(id);

const CLASE_ESTADO = {
    ACTIVO: "activo", VENCIDO: "vencido", FINALIZADO: "finalizado",
    PAGADA: "finalizado",
};

const etiqueta = (estado, texto) => el("span", { class: `etiqueta ${CLASE_ESTADO[estado] ?? ""}` }, texto);
const fechaHora = (iso) => new Date(iso).toLocaleString("es-CO", { dateStyle: "short", timeStyle: "short" });
const filaVacia = (columnas, texto) => el("tr", {}, el("td", { colspan: columnas, class: "vacio" }, texto));

// ---------------- datos ----------------

async function cargar() {
    try {
        const cuenta = await api.get("/mi-cuenta");
        pintarPrestamos(cuenta.prestamos);
        pintarReservas(cuenta.reservas);
        pintarMultas(cuenta.multas);
        mostrarMensaje($("avisoMultas"), textoMultasPendientes(cuenta.totalMultasPendientes));
        await cargarLibrosReservables();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

async function cargarLibrosReservables() {
    const libros = librosReservables(await api.get("/libros"));
    llenarSelect($("idLibro"),
        libros.map((l) => ({ id: l.idLibro, nombre: `${l.titulo} (${l.isbn})` })),
        libros.length === 0 ? "No hay libros para reservar ahora" : "Selecciona un libro...");
    $("btnReservar").disabled = libros.length === 0;
}

// ---------------- tablas ----------------

function pintarPrestamos(prestamos) {
    const cuerpo = $("tbodyPrestamos");
    vaciar(cuerpo);
    if (prestamos.length === 0) {
        cuerpo.append(filaVacia(5, "Todavía no tienes préstamos"));
        return;
    }
    for (const p of prestamos) {
        const acciones = el("td", { class: "acciones" });
        if (puedeRenovar(p)) {
            const boton = el("button", { class: "btn secundario chico", type: "button", onclick: () => renovar(p, boton) }, "Renovar");
            acciones.append(boton);
        }
        cuerpo.append(el("tr", {},
            el("td", {}, [...new Set(p.detalles.map((d) => d.titulo))].join(", ")),
            el("td", {}, formatearFecha(p.fechaLimite)),
            el("td", {}, etiqueta(p.estado, p.estado.charAt(0) + p.estado.slice(1).toLowerCase()),
                p.diasRetraso > 0 ? el("small", {}, ` ${p.diasRetraso} día(s) de retraso`) : ""),
            el("td", {}, textoRenovaciones(p.renovaciones)),
            acciones,
        ));
    }
}

function pintarReservas(reservas) {
    const cuerpo = $("tbodyReservas");
    vaciar(cuerpo);
    if (reservas.length === 0) {
        cuerpo.append(filaVacia(4, "No tienes reservas"));
        return;
    }
    for (const r of reservas) {
        const acciones = el("td", { class: "acciones" });
        if (puedeCancelarReserva(r)) {
            const boton = el("button", { class: "btn peligro chico", type: "button", onclick: () => cancelar(r, boton) }, "Cancelar");
            acciones.append(boton);
        }
        cuerpo.append(el("tr", {},
            el("td", {}, r.titulo),
            el("td", {}, el("span", { class: `etiqueta ${claseEstadoReserva(r.estado)}` }, etiquetaEstadoReserva(r.estado))),
            el("td", {}, textoSituacionReserva(r)),
            acciones,
        ));
    }
}

function pintarMultas(multas) {
    const cuerpo = $("tbodyMultas");
    vaciar(cuerpo);
    if (multas.length === 0) {
        cuerpo.append(filaVacia(4, "No tienes multas"));
        return;
    }
    for (const m of multas) {
        cuerpo.append(el("tr", {},
            el("td", {}, fechaHora(m.fecha)),
            el("td", {}, textoMotivoMulta(m.motivo)),
            el("td", {}, formatoMoneda.format(m.valor)),
            el("td", {}, etiqueta(m.estado, m.estado.charAt(0) + m.estado.slice(1).toLowerCase())),
        ));
    }
}

// ---------------- acciones ----------------

async function renovar(prestamo, boton) {
    boton.disabled = true;
    try {
        const respuesta = await api.put(`/mi-cuenta/prestamos/${prestamo.idPrestamo}/renovar`, {});
        mostrarMensaje($("mensaje"), textoRenovado(respuesta), "ok");
        await cargar();
    } catch (error) {
        // 409: vencido, máximo de renovaciones, multas pendientes o reserva de otra persona
        mostrarMensaje($("mensaje"), textoError(error));
        boton.disabled = false;
    }
}

async function cancelar(reserva, boton) {
    if (!window.confirm(`¿Cancelar tu reserva de "${reserva.titulo}"?`)) return;
    boton.disabled = true;
    try {
        await api.put(`/mi-cuenta/reservas/${reserva.idReserva}/cancelar`, {});
        mostrarMensaje($("mensaje"), "Reserva cancelada", "ok");
        await cargar();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
        boton.disabled = false;
    }
}

async function reservar() {
    const idLibro = $("idLibro").value;
    if (idLibro === "") {
        mostrarMensaje($("mensaje"), "Selecciona el libro que quieres reservar");
        return;
    }
    $("btnReservar").disabled = true;
    try {
        await api.post("/mi-cuenta/reservas", { idLibro: Number(idLibro) });
        mostrarMensaje($("mensaje"), "Reserva registrada. Te toca esperar tu turno en la cola.", "ok");
        await cargar();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
        $("btnReservar").disabled = false;
    }
}

// ---------------- inicio ----------------

function iniciar() {
    const sesion = requireSesion();
    if (!sesion) return;
    $("usuarioActual").textContent = `${sesion.nombres} ${sesion.apellidos} · ${sesion.rol}`;
    $("btnSalir").addEventListener("click", logout);
    $("btnReservar").addEventListener("click", reservar);
    cargar();
}

iniciar();
