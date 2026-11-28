import { api, textoError } from "./api.js";
import { requireSesion } from "./auth.js";
import { el, vaciar, mostrarMensaje } from "./dom.js";
import { cuerpoTipo, usoDeTipo } from "./tipos-formato.js";

const $ = (id) => document.getElementById(id);

let tipos = [];
let editandoId = null; // null = creando, numero = editando

async function cargarTipos() {
    try {
        tipos = await api.get("/tipos-usuario");
        pintarTabla();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

function pintarTabla() {
    const cuerpo = $("tbodyTipos");
    vaciar(cuerpo);
    if (tipos.length === 0) {
        cuerpo.append(el("tr", {}, el("td", { colspan: 6, class: "vacio" }, "No hay tipos de usuario")));
        return;
    }
    for (const t of tipos) {
        const uso = usoDeTipo(t);
        cuerpo.append(el("tr", {},
            el("td", {}, t.nombre),
            el("td", {}, t.maxLibros),
            el("td", {}, t.diasPrestamo),
            el("td", {}, t.maxRenovaciones),
            el("td", {}, uso.texto),
            el("td", { class: "acciones" },
                el("button", { class: "btn secundario chico", type: "button", onclick: () => abrirFormulario(t) }, "Editar"),
                el("button", {
                    class: "btn peligro chico", type: "button", disabled: !uso.eliminable,
                    title: uso.eliminable ? "" : "Tiene usuarios o es el tipo del registro propio",
                    onclick: () => eliminar(t),
                }, "Eliminar"),
            ),
        ));
    }
}

function abrirFormulario(tipo = null) {
    editandoId = tipo ? tipo.idTipoUsuario : null;
    $("tituloDialogo").textContent = tipo ? "Editar tipo de usuario" : "Nuevo tipo de usuario";
    mostrarMensaje($("mensajeForm"), "");
    $("frmTipo").reset();
    $("nombre").disabled = tipo?.nombre === "EXTERNO"; // el del registro propio conserva su nombre
    if (tipo) {
        $("nombre").value = tipo.nombre;
        $("maxLibros").value = tipo.maxLibros;
        $("diasPrestamo").value = tipo.diasPrestamo;
        $("maxRenovaciones").value = tipo.maxRenovaciones;
    }
    $("dlgTipo").showModal();
}

async function guardar(evento) {
    evento.preventDefault();
    const boton = $("btnGuardar");
    boton.disabled = true;
    mostrarMensaje($("mensajeForm"), "");
    try {
        const datos = cuerpoTipo({
            nombre: $("nombre").value, maxLibros: $("maxLibros").value,
            diasPrestamo: $("diasPrestamo").value, maxRenovaciones: $("maxRenovaciones").value,
        });
        const creando = editandoId === null;
        if (creando) await api.post("/tipos-usuario", datos);
        else await api.put(`/tipos-usuario/${editandoId}`, datos);

        $("dlgTipo").close();
        mostrarMensaje($("mensaje"), creando ? "Tipo creado" : "Tipo actualizado", "ok");
        await cargarTipos();
    } catch (error) {
        mostrarMensaje($("mensajeForm"), textoError(error));
    } finally {
        boton.disabled = false;
    }
}

async function eliminar(tipo) {
    if (!window.confirm(`¿Eliminar el tipo ${tipo.nombre}?`)) return;
    try {
        await api.delete(`/tipos-usuario/${tipo.idTipoUsuario}`);
        mostrarMensaje($("mensaje"), "Tipo eliminado", "ok");
        await cargarTipos();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

async function iniciar() {
    const sesion = requireSesion();
    if (!sesion) return;
    // Solo el administrador entra a este módulo
    if (sesion.rol !== "ADMIN") {
        window.location.replace("menu.html");
        return;
    }
    $("btnNuevo").addEventListener("click", () => abrirFormulario());
    $("btnCancelar").addEventListener("click", () => $("dlgTipo").close());
    $("frmTipo").addEventListener("submit", guardar);
    await cargarTipos();
}

iniciar();
