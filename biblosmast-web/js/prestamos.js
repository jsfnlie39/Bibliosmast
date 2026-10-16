import { api, textoError } from "./api.js";
import { requireSesion, logout } from "./auth.js";
import { el, vaciar, mostrarMensaje, llenarSelect } from "./dom.js";

const $ = (id) => document.getElementById(id);

const ROLES_PERSONAL = ["ADMIN", "BIBLIOTECARIO"];
const DIAS_PRESTAMO_POR_DEFECTO = 7;

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
    const [usuarios, ejemplares] = await Promise.all([
        api.get("/usuarios/activos"),
        api.get("/ejemplares/disponibles"),
    ]);
    llenarSelect($("idUsuario"), usuarios, "Selecciona...");
    const seleccionado = $("filtroUsuario").value;
    llenarSelect($("filtroUsuario"), usuarios, "Todos los usuarios");
    $("filtroUsuario").value = seleccionado;
    llenarSelect($("idsEjemplares"), ejemplares);
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
        cuerpo.append(el("tr", {}, el("td", { colspan: 6, class: "vacio" }, "No hay préstamos para mostrar")));
        return;
    }

    for (const p of prestamos) {
        cuerpo.append(el("tr", {},
            el("td", {}, p.idPrestamo),
            el("td", {}, `${p.usuario} (${p.documento})`),
            el("td", {}, formatearFechaHora(p.fechaPrestamo)),
            el("td", {}, formatearFecha(p.fechaLimite)),
            el("td", {}, etiquetaEstado(p)),
            el("td", {}, p.detalles.map((d) => itemDetalle(p, d))),
        ));
    }
}

// ---------------- devolucion ----------------

async function devolver(prestamo, detalle, estadoDevolucion, boton) {
    boton.disabled = true;
    try {
        await api.put(
            `/prestamos/${prestamo.idPrestamo}/detalles/${detalle.idDetalle}/devolucion`,
            { estadoDevolucion },
        );
        mostrarMensaje($("mensaje"), `Ejemplar ${detalle.codigo} devuelto`, "ok");
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
    $("fechaLimite").min = aTextoFecha(sumarDias(1));
    $("fechaLimite").value = aTextoFecha(sumarDias(DIAS_PRESTAMO_POR_DEFECTO));
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
            idBibliotecario: sesion.idUsuario,
            fechaLimite: $("fechaLimite").value || null,
            idsEjemplares: [...$("idsEjemplares").selectedOptions].map((o) => Number(o.value)),
            observaciones: observaciones === "" ? null : observaciones,
        });
        $("dlgPrestamo").close();
        const atendidas = prestamo.reservasAtendidas;
        const aviso = atendidas === 0 ? ""
            : atendidas === 1 ? " Se atendió 1 reserva del usuario."
            : ` Se atendieron ${atendidas} reservas del usuario.`;
        mostrarMensaje($("mensaje"), `Préstamo registrado.${aviso}`, "ok");
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
