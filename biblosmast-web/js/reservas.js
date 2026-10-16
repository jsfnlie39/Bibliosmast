import { api, textoError } from "./api.js";
import { requireSesion, logout } from "./auth.js";
import { el, vaciar, mostrarMensaje, llenarSelect } from "./dom.js";

const $ = (id) => document.getElementById(id);

const ROLES_PERSONAL = ["ADMIN", "BIBLIOTECARIO"];
const DIAS_RESERVA_POR_DEFECTO = 3;
const CLASE_ESTADO = {
    PENDIENTE: "activo", VENCIDA: "vencido", ATENDIDA: "finalizado", CANCELADA: "finalizado",
};

let reservas = [];
let editandoId = null; // null = creando, numero = editando

// ---------------- utilidades ----------------

/** "2026-10-22" -> "22/10/2026" (sin pasar por Date, para evitar corrimientos de zona horaria). */
function formatearFecha(texto) {
    if (!texto) return "—";
    const [anio, mes, dia] = texto.split("-");
    return `${dia}/${mes}/${anio}`;
}

function formatearFechaHora(iso) {
    return new Date(iso).toLocaleString("es-CO", { dateStyle: "short", timeStyle: "short" });
}

/** Date -> "YYYY-MM-DD" en hora local. */
function aTextoFecha(fecha) {
    return fecha.toLocaleDateString("en-CA");
}

function sumarDias(dias) {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + dias);
    return fecha;
}

const etiquetaEstado = (estado) => estado.charAt(0) + estado.slice(1).toLowerCase();

/** Si la opcion no esta en el select (p. ej. un usuario que ya no esta activo), la agrega para poder editar. */
function asegurarOpcion(select, id, texto) {
    if (![...select.options].some((o) => o.value === String(id))) {
        select.append(el("option", { value: id }, texto));
    }
}

// ---------------- datos ----------------

async function cargarOpciones() {
    const [usuarios, libros] = await Promise.all([
        api.get("/usuarios/activos"),
        api.get("/libros"),
    ]);
    llenarSelect($("idUsuario"), usuarios, "Selecciona...");
    llenarSelect($("idLibro"),
        libros.map((l) => ({ id: l.idLibro, nombre: `${l.titulo} (${l.isbn})` })),
        "Selecciona...");
}

async function cargarReservas() {
    try {
        const estado = $("filtroEstado").value;
        reservas = await api.get(estado ? `/reservas?estado=${encodeURIComponent(estado)}` : "/reservas");
        pintarTabla();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

// ---------------- tabla ----------------

function pintarTabla() {
    const cuerpo = $("tbodyReservas");
    vaciar(cuerpo);

    if (reservas.length === 0) {
        cuerpo.append(el("tr", {}, el("td", { colspan: 7, class: "vacio" }, "No hay reservas para mostrar")));
        return;
    }

    for (const r of reservas) {
        const cerrada = r.estado === "ATENDIDA" || r.estado === "CANCELADA";
        const acciones = el("td", { class: "acciones" },
            el("button", { class: "btn secundario chico", type: "button", disabled: cerrada, onclick: () => abrirFormulario(r) }, "Editar"),
            el("button", { class: "btn peligro chico", type: "button", onclick: () => eliminar(r) }, "Eliminar"),
        );
        cuerpo.append(el("tr", {},
            el("td", {}, r.usuario),
            el("td", {}, r.titulo),
            el("td", {}, formatearFechaHora(r.fechaReserva)),
            el("td", {}, formatearFecha(r.fechaVencimiento)),
            el("td", {}, el("span", { class: `etiqueta ${CLASE_ESTADO[r.estado]}` }, etiquetaEstado(r.estado))),
            el("td", {}, r.ejemplaresDisponibles),
            acciones,
        ));
    }
}

// ---------------- formulario ----------------

function abrirFormulario(reserva = null) {
    editandoId = reserva ? reserva.idReserva : null;
    $("tituloDialogo").textContent = reserva ? "Editar reserva" : "Nueva reserva";
    mostrarMensaje($("mensajeForm"), "");
    $("frmReserva").reset();
    $("grupoEstado").hidden = reserva === null; // al crear siempre queda pendiente
    $("fechaVencimiento").min = aTextoFecha(sumarDias(1));

    if (reserva) {
        asegurarOpcion($("idUsuario"), reserva.idUsuario, `${reserva.usuario} (no activo)`);
        $("idUsuario").value = reserva.idUsuario;
        $("idLibro").value = reserva.idLibro;
        $("fechaVencimiento").value = reserva.fechaVencimiento;
        // "Vencida" no es un estado que se pueda elegir: sigue siendo pendiente hasta que se atienda o cancele
        $("estado").value = reserva.estado === "VENCIDA" ? "PENDIENTE" : reserva.estado;
    } else {
        $("fechaVencimiento").value = aTextoFecha(sumarDias(DIAS_RESERVA_POR_DEFECTO));
    }
    $("dlgReserva").showModal();
}

async function guardar(evento) {
    evento.preventDefault();
    const boton = $("btnGuardar");
    boton.disabled = true;
    mostrarMensaje($("mensajeForm"), "");
    try {
        const creando = editandoId === null;
        const datos = {
            idUsuario: $("idUsuario").value === "" ? null : Number($("idUsuario").value),
            idLibro: $("idLibro").value === "" ? null : Number($("idLibro").value),
            fechaVencimiento: $("fechaVencimiento").value || null,
            estado: creando ? null : $("estado").value,
        };
        if (creando) await api.post("/reservas", datos);
        else await api.put(`/reservas/${editandoId}`, datos);

        $("dlgReserva").close();
        mostrarMensaje($("mensaje"), creando ? "Reserva registrada" : "Reserva actualizada", "ok");
        await cargarReservas();
    } catch (error) {
        mostrarMensaje($("mensajeForm"), textoError(error));
    } finally {
        boton.disabled = false;
    }
}

async function eliminar(reserva) {
    if (!window.confirm(`¿Eliminar la reserva de ${reserva.usuario} para "${reserva.titulo}"?`)) return;
    try {
        await api.delete(`/reservas/${reserva.idReserva}`);
        mostrarMensaje($("mensaje"), "Reserva eliminada", "ok");
        await cargarReservas();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

// ---------------- inicio ----------------

async function iniciar() {
    const sesion = requireSesion();
    if (!sesion) return;

    // Solo el personal de la biblioteca entra a este modulo.
    if (!ROLES_PERSONAL.includes(sesion.rol)) {
        window.location.replace("menu.html");
        return;
    }

    $("usuarioActual").textContent = `${sesion.nombres} ${sesion.apellidos} · ${sesion.rol}`;
    $("btnSalir").addEventListener("click", logout);
    $("btnNueva").addEventListener("click", () => abrirFormulario());
    $("btnCancelar").addEventListener("click", () => $("dlgReserva").close());
    $("frmReserva").addEventListener("submit", guardar);
    $("filtroEstado").addEventListener("change", cargarReservas);

    try {
        await cargarOpciones();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
    await cargarReservas();
}

iniciar();
