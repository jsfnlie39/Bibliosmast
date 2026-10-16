import { api, textoError } from "./api.js";
import { requireSesion, logout } from "./auth.js";
import { el, vaciar, mostrarMensaje } from "./dom.js";

const $ = (id) => document.getElementById(id);

const ROLES_PERSONAL = ["ADMIN", "BIBLIOTECARIO"];

/**
 * CRUD generico: cada catalogo se describe con su ruta, su campo id y sus campos.
 * Para agregar otro catalogo sencillo basta con sumar una entrada aqui.
 */
const CATALOGOS = {
    autores: {
        titulo: "Autores", singular: "autor", ruta: "/autores", id: "idAutor",
        nombreItem: (x) => `${x.nombres} ${x.apellidos}`,
        campos: [
            { name: "nombres", label: "Nombres", requerido: true, max: 80 },
            { name: "apellidos", label: "Apellidos", requerido: true, max: 80 },
            { name: "nacionalidad", label: "Nacionalidad", max: 60 },
        ],
    },
    categorias: {
        titulo: "Categorías", singular: "categoría", ruta: "/categorias", id: "idCategoria",
        nombreItem: (x) => x.nombre,
        campos: [
            { name: "nombre", label: "Nombre", requerido: true, max: 60 },
            { name: "descripcion", label: "Descripción", max: 150 },
        ],
    },
    editoriales: {
        titulo: "Editoriales", singular: "editorial", ruta: "/editoriales", id: "idEditorial",
        nombreItem: (x) => x.nombre,
        campos: [
            { name: "nombre", label: "Nombre", requerido: true, max: 100 },
            { name: "pais", label: "País", max: 60 },
        ],
    },
};

let actual = "autores";
let items = [];
let editandoId = null; // null = creando, numero = editando

// ---------------- pestañas y tabla ----------------

function pintarPestanas() {
    const contenedor = $("pestanas");
    vaciar(contenedor);
    for (const [clave, cat] of Object.entries(CATALOGOS)) {
        contenedor.append(el("button", {
            class: clave === actual ? "pestana activa" : "pestana",
            type: "button", role: "tab",
            "aria-selected": clave === actual ? "true" : "false",
            onclick: () => cambiarCatalogo(clave),
        }, cat.titulo));
    }
}

async function cambiarCatalogo(clave) {
    actual = clave;
    mostrarMensaje($("mensaje"), "");
    pintarPestanas();
    await cargarItems();
}

async function cargarItems() {
    const cat = CATALOGOS[actual];
    try {
        items = await api.get(cat.ruta);
        pintarTabla();
    } catch (error) {
        items = [];
        pintarTabla();
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

function pintarTabla() {
    const cat = CATALOGOS[actual];

    const encabezado = $("encabezado");
    vaciar(encabezado);
    for (const campo of cat.campos) encabezado.append(el("th", {}, campo.label));
    encabezado.append(el("th", {}));

    const cuerpo = $("cuerpo");
    vaciar(cuerpo);
    if (items.length === 0) {
        cuerpo.append(el("tr", {}, el("td", { colspan: cat.campos.length + 1, class: "vacio" }, "No hay registros para mostrar")));
        return;
    }
    for (const item of items) {
        const fila = el("tr", {});
        for (const campo of cat.campos) fila.append(el("td", {}, item[campo.name] ?? "—"));
        fila.append(el("td", { class: "acciones" },
            el("button", { class: "btn secundario chico", type: "button", onclick: () => abrirFormulario(item) }, "Editar"),
            el("button", { class: "btn peligro chico", type: "button", onclick: () => eliminar(item) }, "Eliminar"),
        ));
        cuerpo.append(fila);
    }
}

// ---------------- formulario ----------------

function abrirFormulario(item = null) {
    const cat = CATALOGOS[actual];
    editandoId = item ? item[cat.id] : null;
    $("tituloDialogo").textContent = `${item ? "Editar" : "Nuevo registro de"} ${cat.singular}`;
    mostrarMensaje($("mensajeForm"), "");

    const contenedor = $("camposForm");
    vaciar(contenedor);
    for (const campo of cat.campos) {
        const id = `campo_${campo.name}`;
        contenedor.append(el("div", { class: "campo" },
            el("label", { for: id }, campo.requerido ? `${campo.label} *` : campo.label),
            el("input", { id, name: campo.name, type: "text", maxlength: campo.max, value: item?.[campo.name] ?? "" }),
        ));
    }
    $("dlgItem").showModal();
}

function leerFormulario() {
    const datos = {};
    for (const campo of CATALOGOS[actual].campos) {
        const valor = $(`campo_${campo.name}`).value.trim();
        datos[campo.name] = valor === "" ? null : valor;
    }
    return datos;
}

async function guardar(evento) {
    evento.preventDefault();
    const cat = CATALOGOS[actual];
    const boton = $("btnGuardar");
    boton.disabled = true;
    mostrarMensaje($("mensajeForm"), "");
    try {
        const creando = editandoId === null;
        if (creando) await api.post(cat.ruta, leerFormulario());
        else await api.put(`${cat.ruta}/${editandoId}`, leerFormulario());

        $("dlgItem").close();
        mostrarMensaje($("mensaje"), creando ? "Registro creado" : "Registro actualizado", "ok");
        await cargarItems();
    } catch (error) {
        mostrarMensaje($("mensajeForm"), textoError(error));
    } finally {
        boton.disabled = false;
    }
}

async function eliminar(item) {
    const cat = CATALOGOS[actual];
    if (!window.confirm(`¿Eliminar ${cat.singular} «${cat.nombreItem(item)}»?`)) return;
    try {
        await api.delete(`${cat.ruta}/${item[cat.id]}`);
        mostrarMensaje($("mensaje"), "Registro eliminado", "ok");
        await cargarItems();
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
    $("btnCancelar").addEventListener("click", () => $("dlgItem").close());
    $("frmItem").addEventListener("submit", guardar);

    pintarPestanas();
    await cargarItems();
}

iniciar();
