import { api, textoError } from "./api.js";
import { requireSesion, logout } from "./auth.js";
import { el, vaciar, mostrarMensaje, llenarSelect } from "./dom.js";

const $ = (id) => document.getElementById(id);

let sesion = null;
let usuarios = [];
let editandoId = null; // null = creando, numero = editando

// ---------------- datos ----------------

async function cargarRoles() {
    llenarSelect($("idRol"), await api.get("/roles"), "Selecciona...");
}

async function cargarUsuarios(busqueda = "") {
    try {
        const ruta = busqueda ? `/usuarios?q=${encodeURIComponent(busqueda)}` : "/usuarios";
        usuarios = await api.get(ruta);
        pintarTabla();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

// ---------------- tabla ----------------

function pintarTabla() {
    const cuerpo = $("tbodyUsuarios");
    vaciar(cuerpo);

    if (usuarios.length === 0) {
        cuerpo.append(el("tr", {}, el("td", { colspan: 7, class: "vacio" }, "No hay usuarios para mostrar")));
        return;
    }

    for (const u of usuarios) {
        const acciones = el("td", { class: "acciones" },
            el("button", { class: "btn secundario chico", type: "button", onclick: () => abrirFormulario(u) }, "Editar"),
        );
        // No se ofrece eliminar la propia cuenta
        if (u.idUsuario !== sesion.idUsuario) {
            acciones.append(
                el("button", { class: "btn peligro chico", type: "button", onclick: () => eliminar(u) }, "Eliminar"),
            );
        }

        const estadoClase = u.estado === "ACTIVO" ? "activo" : u.estado === "SANCIONADO" ? "vencido" : "finalizado";
        cuerpo.append(el("tr", {},
            el("td", {}, u.documento),
            el("td", {}, `${u.nombres} ${u.apellidos}`),
            el("td", {}, u.email),
            el("td", {}, u.username ?? "— sin cuenta —"),
            el("td", {}, u.rol ?? "—"),
            el("td", {}, el("span", { class: `etiqueta ${estadoClase}` }, u.estado)),
            acciones,
        ));
    }
}

// ---------------- formulario ----------------

function abrirFormulario(usuario = null) {
    editandoId = usuario ? usuario.idUsuario : null;
    $("tituloDialogo").textContent = usuario ? "Editar usuario" : "Nuevo usuario";
    mostrarMensaje($("mensajeForm"), "");
    $("frmUsuario").reset();

    const editando = usuario !== null;
    $("notaPassword").hidden = editando;
    $("ayudaPassword").textContent = editando
        ? "Déjala vacía para no cambiarla. Si la escribes, mínimo 8 caracteres."
        : "Mínimo 8 caracteres.";

    if (usuario) {
        $("documento").value = usuario.documento;
        $("estado").value = usuario.estado;
        $("nombres").value = usuario.nombres;
        $("apellidos").value = usuario.apellidos;
        $("email").value = usuario.email;
        $("telefono").value = usuario.telefono ?? "";
        $("username").value = usuario.username ?? "";
        $("idRol").value = usuario.idRol ?? "";
        $("activo").checked = usuario.activo ?? true;
    }
    $("dlgUsuario").showModal();
}

const textoONull = (valor) => (valor.trim() === "" ? null : valor.trim());

function leerFormulario() {
    return {
        documento: $("documento").value.trim(),
        nombres: $("nombres").value.trim(),
        apellidos: $("apellidos").value.trim(),
        email: $("email").value.trim(),
        telefono: textoONull($("telefono").value),
        estado: $("estado").value,
        username: $("username").value.trim(),
        password: $("password").value === "" ? null : $("password").value,
        idRol: $("idRol").value === "" ? null : Number($("idRol").value),
        activo: $("activo").checked,
    };
}

async function guardar(evento) {
    evento.preventDefault();
    const boton = $("btnGuardar");
    boton.disabled = true;
    mostrarMensaje($("mensajeForm"), "");
    try {
        const datos = leerFormulario();
        const creando = editandoId === null;
        if (creando) await api.post("/usuarios", datos);
        else await api.put(`/usuarios/${editandoId}`, datos);

        $("dlgUsuario").close();
        mostrarMensaje($("mensaje"), creando ? "Usuario registrado" : "Usuario actualizado", "ok");
        await cargarUsuarios($("busqueda").value.trim());
    } catch (error) {
        mostrarMensaje($("mensajeForm"), textoError(error));
    } finally {
        boton.disabled = false;
    }
}

async function eliminar(usuario) {
    if (!window.confirm(`¿Eliminar a ${usuario.nombres} ${usuario.apellidos}? Esta acción no se puede deshacer.`)) return;
    try {
        await api.delete(`/usuarios/${usuario.idUsuario}`);
        mostrarMensaje($("mensaje"), "Usuario eliminado", "ok");
        await cargarUsuarios($("busqueda").value.trim());
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

    // Solo el administrador entra a este modulo.
    if (sesion.rol !== "ADMIN") {
        window.location.replace("menu.html");
        return;
    }

    $("usuarioActual").textContent = `${sesion.nombres} ${sesion.apellidos} · ${sesion.rol}`;
    $("btnSalir").addEventListener("click", logout);
    $("btnNuevo").addEventListener("click", () => abrirFormulario());
    $("btnCancelar").addEventListener("click", () => $("dlgUsuario").close());
    $("frmUsuario").addEventListener("submit", guardar);
    $("busqueda").addEventListener("input", conRetardo((e) => cargarUsuarios(e.target.value.trim()), 300));

    try {
        await cargarRoles();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
    await cargarUsuarios();
}

iniciar();
