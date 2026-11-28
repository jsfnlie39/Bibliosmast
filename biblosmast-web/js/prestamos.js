import { api, textoError } from "./api.js";
import { requireSesion, logout } from "./auth.js";
import { el, vaciar, mostrarMensaje, llenarSelect } from "./dom.js";
import { textoMultasGeneradas } from "./multas-formato.js";
import { puedeRenovar, textoRenovaciones, textoRenovado } from "./prestamos-formato.js";
import { textoApartado } from "./reservas-formato.js";

const $ = (id) => document.getElementById(id);

const ROLES_PERSONAL = ["ADMIN", "BIBLIOTECARIO"];

let sesion = null;

// ---------------- utilidades de fecha ----------------

/** "2026-10-22" -> "22/10/2026" (sin pasar por Date, para evitar corrimientos de zona horaria). */
function formatearFecha(texto) {
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

// ---------------- datos ----------------

async function cargarOpciones() {
    const usuarios = await api.get("/usuarios/activos");
    const usuarioForm = $("idUsuario").value;
    llenarSelect($("idUsuario"), usuarios, "Selecciona...");
    $("idUsuario").value = usuarioForm;
    const seleccionado = $("filtroUsuario").value;
    llenarSelect($("filtroUsuario"), usuarios, "Todos los usuarios");
    $("filtroUsuario").value = seleccionado;
    await cargarEjemplares();
}

/**
 * Ejemplares que se pueden prestar. Al elegir usuario se suman los que tiene apartados por una reserva
 * lista para recoger; los ya marcados se conservan si siguen en la lista.
 */
async function cargarEjemplares() {
    const idUsuario = $("idUsuario").value;
    const marcados = new Set([...$("idsEjemplares").selectedOptions].map((o) => o.value));
    const ruta = idUsuario ? `/ejemplares/disponibles?idUsuario=${encodeURIComponent(idUsuario)}` : "/ejemplares/disponibles";
    llenarSelect($("idsEjemplares"), await api.get(ruta));
    for (const opcion of $("idsEjemplares").options) opcion.selected = marcados.has(opcion.value);
}

async function cargarPrestamos() {
    try {
        const parametros = new URLSearchParams();
        if ($("filtroEstado").value) parametros.set("estado", $("filtroEstado").value);
        if ($("filtroUsuario").value) parametros.set("idUsuario", $("filtroUsuario").value);
        const consulta = parametros.toString();
        pintarTabla(await api.get(consulta ? `/prestamos?${consulta}` : "/prestamos"));
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

// ---------------- tabla ----------------

function etiquetaEstado(prestamo) {
    const texto = prestamo.estado === "VENCIDO"
        ? `Vencido (${prestamo.diasRetraso} d)`
        : prestamo.estado === "ACTIVO" ? "Activo" : "Finalizado";
    return el("span", { class: `etiqueta ${prestamo.estado.toLowerCase()}` }, texto);
}

function itemDetalle(prestamo, detalle) {
    const item = el("div", { class: "detalle-item" }, el("span", {}, `${detalle.codigo} · ${detalle.titulo}`));

    if (detalle.fechaDevolucion) {
        const estado = detalle.estadoDevolucion.replace("_", " ").toLowerCase();
        item.append(el("small", {}, `— devuelto ${formatearFechaHora(detalle.fechaDevolucion)} (${estado})`));
        return item;
    }

    item.append(el("small", {}, "— pendiente"));
    const selector = el("select", { "aria-label": "Estado de devolución" },
        el("option", { value: "BUEN_ESTADO" }, "Buen estado"),
        el("option", { value: "DANADO" }, "Dañado"),
        el("option", { value: "PERDIDO" }, "Perdido"),
    );
    const boton = el("button", {
        class: "btn secundario chico", type: "button",
        onclick: () => devolver(prestamo, detalle, selector.value, boton),
    }, "Devolver");
    item.append(selector, boton);
    return item;
}

function pintarTabla(prestamos) {
    const cuerpo = $("tbodyPrestamos");
    vaciar(cuerpo);

    if (prestamos.length === 0) {
        cuerpo.append(el("tr", {}, el("td", { colspan: 8, class: "vacio" }, "No hay préstamos para mostrar")));
        return;
    }

    for (const p of prestamos) {
        const acciones = el("td", { class: "acciones" });
        if (puedeRenovar(p)) {
            const botonRenovar = el("button", {
                class: "btn secundario chico", type: "button",
                onclick: () => renovar(p, botonRenovar),
            }, "Renovar");
            acciones.append(botonRenovar);
        }
        cuerpo.append(el("tr", {},
            el("td", {}, p.idPrestamo),
            el("td", {}, el("a", { class: "ficha-vinculo", href: `ficha-usuario.html?id=${p.idUsuario}` }, `${p.usuario} (${p.documento})`)),
            el("td", {}, formatearFechaHora(p.fechaPrestamo)),
            el("td", {}, formatearFecha(p.fechaLimite)),
            el("td", {}, etiquetaEstado(p)),
            el("td", {}, textoRenovaciones(p.renovaciones)),
            el("td", {}, p.detalles.map((d) => itemDetalle(p, d))),
            acciones,
        ));
    }
}

// ---------------- renovacion ----------------

async function renovar(prestamo, boton) {
    if (!window.confirm(`¿Renovar el préstamo N° ${prestamo.idPrestamo} de ${prestamo.usuario}?`)) return;
    boton.disabled = true;
    try {
        const respuesta = await api.put(`/prestamos/${prestamo.idPrestamo}/renovar`, {});
        mostrarMensaje($("mensaje"), textoRenovado(respuesta), "ok");
        await cargarPrestamos();
    } catch (error) {
        // 409: multas pendientes, reserva de otra persona, máximo de renovaciones, vencido o ya devuelto
        mostrarMensaje($("mensaje"), textoError(error));
        boton.disabled = false;
    }
}

// ---------------- devolucion ----------------

async function devolver(prestamo, detalle, estadoDevolucion, boton) {
    boton.disabled = true;
    try {
        const respuesta = await api.put(
            `/prestamos/${prestamo.idPrestamo}/detalles/${detalle.idDetalle}/devolucion`,
            { estadoDevolucion },
        );
        mostrarMensaje($("mensaje"), `Ejemplar ${detalle.codigo} devuelto.${textoMultasGeneradas(respuesta)}${textoApartado(respuesta)}`, "ok");
        await Promise.all([cargarPrestamos(), cargarOpciones()]);
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
        boton.disabled = false;
    }
}

// ---------------- nuevo prestamo ----------------

function abrirFormulario() {
    mostrarMensaje($("mensajeForm"), "");
    $("frmPrestamo").reset();
    cargarEjemplares().catch(() => {}); // tras reset(): sin usuario elegido, solo los disponibles
    $("fechaLimite").min = aTextoFecha(sumarDias(1));
    $("dlgPrestamo").showModal();
}

async function guardar(evento) {
    evento.preventDefault();
    const boton = $("btnGuardar");
    boton.disabled = true;
    mostrarMensaje($("mensajeForm"), "");
    try {
        const observaciones = $("observaciones").value.trim();
        const prestamo = await api.post("/prestamos", {
            idUsuario: $("idUsuario").value === "" ? null : Number($("idUsuario").value),
            fechaLimite: $("fechaLimite").value || null,
            idsEjemplares: [...$("idsEjemplares").selectedOptions].map((o) => Number(o.value)),
            observaciones: observaciones === "" ? null : observaciones,
        });
        $("dlgPrestamo").close();
        const atendidas = prestamo.reservasAtendidas;
        const aviso = atendidas === 0 ? ""
            : atendidas === 1 ? " Se atendió 1 reserva del usuario."
            : ` Se atendieron ${atendidas} reservas del usuario.`;
        mostrarMensaje($("mensaje"), `Préstamo registrado. Debe devolverse antes del ${formatearFecha(prestamo.fechaLimite)}.${aviso}`, "ok");
        await Promise.all([cargarPrestamos(), cargarOpciones()]);
    } catch (error) {
        mostrarMensaje($("mensajeForm"), textoError(error));
    } finally {
        boton.disabled = false;
    }
}

// ---------------- inicio ----------------

async function iniciar() {
    sesion = requireSesion();
    if (!sesion) return;

    // Solo el personal de la biblioteca entra a este modulo.
    if (!ROLES_PERSONAL.includes(sesion.rol)) {
        window.location.replace("menu.html");
        return;
    }

    $("usuarioActual").textContent = `${sesion.nombres} ${sesion.apellidos} · ${sesion.rol}`;
    $("btnSalir").addEventListener("click", logout);
    $("btnNuevo").addEventListener("click", abrirFormulario);
    $("btnCancelar").addEventListener("click", () => $("dlgPrestamo").close());
    $("frmPrestamo").addEventListener("submit", guardar);
    $("idUsuario").addEventListener("change", () => cargarEjemplares().catch((e) => mostrarMensaje($("mensajeForm"), textoError(e))));
    $("filtroEstado").addEventListener("change", cargarPrestamos);
    $("filtroUsuario").addEventListener("change", cargarPrestamos);

    try {
        await cargarOpciones();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
    await cargarPrestamos();
}

iniciar();
