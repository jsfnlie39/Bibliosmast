import { api, textoError } from "./api.js";
import { requireSesion, logout } from "./auth.js";
import { esAdmin } from "./sesion.js";
import { el, vaciar, mostrarMensaje, llenarSelect } from "./dom.js";
import { hayFiltros, parametrosBusqueda, opcionesDeFiltro } from "./ficha-formato.js";

const $ = (id) => document.getElementById(id);

let sesion = null;
let puedeEditar = false;
let libros = [];
let editandoId = null; // null = creando, numero = editando

// ---------------- datos ----------------

async function cargarCatalogos() {
    const [categorias, editoriales, autores] = await Promise.all([
        api.get("/opciones/categorias"),
        api.get("/opciones/editoriales"),
        api.get("/opciones/autores"),
    ]);
    llenarSelect($("idCategoria"), categorias, "Selecciona...");
    llenarSelect($("idEditorial"), editoriales, "Selecciona...");
    llenarSelect($("idsAutores"), autores);
}

/** Lee los filtros del formulario (texto y búsqueda avanzada). */
function leerFiltros() {
    return {
        q: $("busqueda").value.trim(),
        idAutor: $("fAutor").value,
        idCategoria: $("fCategoria").value,
        idEditorial: $("fEditorial").value,
        idioma: $("fIdioma").value,
        anioDesde: $("fAnioDesde").value,
        anioHasta: $("fAnioHasta").value,
        soloDisponibles: $("fSoloDisponibles").checked,
    };
}

/** Los selectores de filtro salen de los libros sin filtrar, así sirven también para quien no ve los catálogos del personal. */
function llenarFiltros(todos) {
    const o = opcionesDeFiltro(todos);
    llenarSelect($("fAutor"), o.autores, "Todos los autores");
    llenarSelect($("fCategoria"), o.categorias, "Todas las categorías");
    llenarSelect($("fEditorial"), o.editoriales, "Todas las editoriales");
    llenarSelect($("fIdioma"), o.idiomas, "Todos los idiomas");
}

function limpiarFiltros() {
    $("busqueda").value = "";
    for (const id of ["fAutor", "fCategoria", "fEditorial", "fIdioma", "fAnioDesde", "fAnioHasta"]) $(id).value = "";
    $("fSoloDisponibles").checked = false;
    return cargarLibros();
}

async function cargarLibros() {
    try {
        const filtros = leerFiltros();
        libros = await api.get(`/libros${parametrosBusqueda(filtros)}`);
        if (!hayFiltros(filtros)) llenarFiltros(libros);
        pintarTabla();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

// ---------------- tabla ----------------

function pintarTabla() {
    const cuerpo = $("tbodyLibros");
    vaciar(cuerpo);

    if (libros.length === 0) {
        cuerpo.append(el("tr", {}, el("td", { colspan: 8, class: "vacio" },
            hayFiltros(leerFiltros()) ? "Ningún libro coincide con los filtros" : "No hay libros para mostrar")));
        return;
    }

    for (const libro of libros) {
        const acciones = el("td", { class: "acciones" });
        if (puedeEditar) {
            acciones.append(
                el("a", { class: "btn secundario chico", href: `ficha-libro.html?id=${libro.idLibro}` }, "Ficha"),
                el("button", { class: "btn secundario chico", type: "button", onclick: () => abrirFormulario(libro) }, "Editar"),
                ...(esAdmin() ? [el("button", { class: "btn peligro chico", type: "button", onclick: () => eliminar(libro) }, "Eliminar")] : []),
            );
        }
        cuerpo.append(el("tr", {},
            el("td", {}, libro.isbn),
            el("td", {}, libro.titulo),
            el("td", {}, libro.autores.join(", ")),
            el("td", {}, libro.categoria),
            el("td", {}, libro.editorial),
            el("td", {}, libro.anioPublicacion ?? "—"),
            el("td", {}, `${libro.ejemplaresDisponibles} de ${libro.totalEjemplares}`),
            acciones,
        ));
    }
}

// ---------------- formulario ----------------

function abrirFormulario(libro = null) {
    editandoId = libro ? libro.idLibro : null;
    $("tituloDialogo").textContent = libro ? "Editar libro" : "Nuevo libro";
    mostrarMensaje($("mensajeForm"), "");
    $("frmLibro").reset();

    if (libro) {
        $("isbn").value = libro.isbn;
        $("titulo").value = libro.titulo;
        $("idCategoria").value = libro.idCategoria;
        $("idEditorial").value = libro.idEditorial;
        $("anioPublicacion").value = libro.anioPublicacion ?? "";
        $("edicion").value = libro.edicion ?? "";
        $("idioma").value = libro.idioma ?? "";
        $("numeroPaginas").value = libro.numeroPaginas ?? "";
        $("descripcion").value = libro.descripcion ?? "";
        for (const opcion of $("idsAutores").options) {
            opcion.selected = libro.idsAutores.includes(Number(opcion.value));
        }
    }
    $("dlgLibro").showModal();
}

const numeroONull = (valor) => (valor === "" ? null : Number(valor));
const textoONull = (valor) => (valor.trim() === "" ? null : valor.trim());

function leerFormulario() {
    return {
        isbn: $("isbn").value.trim(),
        titulo: $("titulo").value.trim(),
        idCategoria: numeroONull($("idCategoria").value),
        idEditorial: numeroONull($("idEditorial").value),
        anioPublicacion: numeroONull($("anioPublicacion").value),
        edicion: textoONull($("edicion").value),
        idioma: textoONull($("idioma").value),
        numeroPaginas: numeroONull($("numeroPaginas").value),
        descripcion: textoONull($("descripcion").value),
        idsAutores: [...$("idsAutores").selectedOptions].map((o) => Number(o.value)),
    };
}

async function guardar(evento) {
    evento.preventDefault();
    const boton = $("btnGuardar");
    boton.disabled = true;
    mostrarMensaje($("mensajeForm"), "");
    try {
        const datos = leerFormulario();
        if (editandoId === null) await api.post("/libros", datos);
        else await api.put(`/libros/${editandoId}`, datos);

        $("dlgLibro").close();
        mostrarMensaje($("mensaje"), editandoId === null ? "Libro registrado" : "Libro actualizado", "ok");
        await cargarLibros();
    } catch (error) {
        mostrarMensaje($("mensajeForm"), textoError(error));
    } finally {
        boton.disabled = false;
    }
}

async function eliminar(libro) {
    if (!window.confirm(`¿Eliminar el libro "${libro.titulo}"?`)) return;
    try {
        await api.delete(`/libros/${libro.idLibro}`);
        mostrarMensaje($("mensaje"), "Libro eliminado", "ok");
        await cargarLibros();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

// ---------------- inicio ----------------

function conRetardo(funcion, ms) {
    let temporizador;
    return (...args) => {
        clearTimeout(temporizador);
        temporizador = setTimeout(() => funcion(...args), ms);
    };
}

async function iniciar() {
    sesion = requireSesion();
    if (!sesion) return;

    // Solo para mostrar u ocultar botones segun el rol.
    puedeEditar = ["ADMIN", "BIBLIOTECARIO"].includes(sesion.rol);

    $("usuarioActual").textContent = `${sesion.nombres} ${sesion.apellidos} · ${sesion.rol}`;
    $("btnSalir").addEventListener("click", logout);
    $("btnNuevo").hidden = !puedeEditar;
    $("btnNuevo").addEventListener("click", () => abrirFormulario());
    $("btnCancelar").addEventListener("click", () => $("dlgLibro").close());
    $("frmLibro").addEventListener("submit", guardar);
    const recargar = conRetardo(() => cargarLibros(), 300);
    $("busqueda").addEventListener("input", recargar);
    for (const id of ["fAutor", "fCategoria", "fEditorial", "fIdioma", "fAnioDesde", "fAnioHasta", "fSoloDisponibles"]) {
        $(id).addEventListener("change", recargar);
    }
    $("btnLimpiar").addEventListener("click", limpiarFiltros);

    // Los catálogos del formulario son del personal: quien solo consulta no los pide (daría error 403)
    if (puedeEditar) {
        try {
            await cargarCatalogos();
        } catch (error) {
            mostrarMensaje($("mensaje"), textoError(error));
        }
    }
    await cargarLibros();
}

iniciar();
