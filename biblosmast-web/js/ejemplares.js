import { api, textoError } from "./api.js";
import { requireSesion, logout } from "./auth.js";
import { el, vaciar, mostrarMensaje, llenarSelect } from "./dom.js";

const $ = (id) => document.getElementById(id);

const ROLES_PERSONAL = ["ADMIN", "BIBLIOTECARIO"];
const CLASE_ESTADO = {
    DISPONIBLE: "activo", PRESTADO: "finalizado", RESERVADO: "finalizado", DANADO: "vencido", PERDIDO: "vencido",
};

let ejemplares = [];
let editandoId = null; // null = creando, numero = editando

// ---------------- utilidades ----------------

/** "2026-10-22" -> "22/10/2026" (sin pasar por Date, para evitar corrimientos de zona horaria). */
function formatearFecha(texto) {
    if (!texto) return "—";
    const [anio, mes, dia] = texto.split("-");
    return `${dia}/${mes}/${anio}`;
}

const etiquetaEstado = (estado) =>
    estado.charAt(0) + estado.slice(1).toLowerCase().replace("danado", "dañado");

// ---------------- datos ----------------

async function cargarLibros() {
    const libros = await api.get("/libros");
    const opciones = libros.map((l) => ({ id: l.idLibro, nombre: `${l.titulo} (${l.isbn})` }));
    llenarSelect($("filtroLibro"), opciones, "Todos los libros");
    llenarSelect($("idLibro"), opciones, "Selecciona...");
}

async function cargarEjemplares() {
    try {
        const idLibro = $("filtroLibro").value;
        ejemplares = await api.get(idLibro ? `/ejemplares?idLibro=${encodeURIComponent(idLibro)}` : "/ejemplares");
        pintarTabla();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

// ---------------- tabla ----------------

function pintarTabla() {
    const cuerpo = $("tbodyEjemplares");
    vaciar(cuerpo);

    if (ejemplares.length === 0) {
        cuerpo.append(el("tr", {}, el("td", { colspan: 6, class: "vacio" }, "No hay ejemplares para mostrar")));
        return;
    }

    for (const e of ejemplares) {
        const prestado = e.estado === "PRESTADO";
        const acciones = el("td", { class: "acciones" },
            el("button", { class: "btn secundario chico", type: "button", disabled: prestado, onclick: () => abrirFormulario(e) }, "Editar"),
            el("button", { class: "btn peligro chico", type: "button", disabled: prestado, onclick: () => eliminar(e) }, "Eliminar"),
        );
        cuerpo.append(el("tr", {},
            el("td", {}, e.codigo),
            el("td", {}, e.titulo),
            el("td", {}, e.ubicacion ?? "—"),
            el("td", {}, el("span", { class: `etiqueta ${CLASE_ESTADO[e.estado]}` }, etiquetaEstado(e.estado))),
            el("td", {}, formatearFecha(e.fechaIngreso)),
            acciones,
        ));
    }
}

// ---------------- formulario ----------------

function abrirFormulario(ejemplar = null) {
    editandoId = ejemplar ? ejemplar.idEjemplar : null;
    $("tituloDialogo").textContent = ejemplar ? "Editar ejemplar" : "Nuevo ejemplar";
    mostrarMensaje($("mensajeForm"), "");
    $("frmEjemplar").reset();

    if (ejemplar) {
        $("idLibro").value = ejemplar.idLibro;
        $("codigo").value = ejemplar.codigo;
        $("ubicacion").value = ejemplar.ubicacion ?? "";
        $("estado").value = ejemplar.estado;
    } else if ($("filtroLibro").value) {
        $("idLibro").value = $("filtroLibro").value; // comodidad: preselecciona el libro filtrado
    }
    $("dlgEjemplar").showModal();
}

async function guardar(evento) {
    evento.preventDefault();
    const boton = $("btnGuardar");
    boton.disabled = true;
    mostrarMensaje($("mensajeForm"), "");
    try {
        const ubicacion = $("ubicacion").value.trim();
        const datos = {
            idLibro: $("idLibro").value === "" ? null : Number($("idLibro").value),
            codigo: $("codigo").value.trim(),
            ubicacion: ubicacion === "" ? null : ubicacion,
            estado: $("estado").value,
        };
        const creando = editandoId === null;
        if (creando) await api.post("/ejemplares", datos);
        else await api.put(`/ejemplares/${editandoId}`, datos);

        $("dlgEjemplar").close();
        mostrarMensaje($("mensaje"), creando ? "Ejemplar registrado" : "Ejemplar actualizado", "ok");
        await cargarEjemplares();
    } catch (error) {
        mostrarMensaje($("mensajeForm"), textoError(error));
    } finally {
        boton.disabled = false;
    }
}

async function eliminar(ejemplar) {
    if (!window.confirm(`¿Eliminar el ejemplar ${ejemplar.codigo}?`)) return;
    try {
        await api.delete(`/ejemplares/${ejemplar.idEjemplar}`);
        mostrarMensaje($("mensaje"), "Ejemplar eliminado", "ok");
        await cargarEjemplares();
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
    $("btnNuevo").addEventListener("click", () => abrirFormulario());
    $("btnCancelar").addEventListener("click", () => $("dlgEjemplar").close());
    $("frmEjemplar").addEventListener("submit", guardar);
    $("filtroLibro").addEventListener("change", cargarEjemplares);

    try {
        await cargarLibros();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
    await cargarEjemplares();
}

iniciar();
