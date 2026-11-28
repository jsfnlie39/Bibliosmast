import { api, textoError } from "./api.js";
import { requireSesion, logout } from "./auth.js";
import { mostrarMensaje } from "./dom.js";
import { guardarSesion } from "./sesion.js";

const $ = (id) => document.getElementById(id);

async function cargarPerfil() {
    const perfil = await api.get("/perfil");
    $("datoUsuario").textContent = perfil.username;
    $("datoRol").textContent = perfil.rol;
    $("datoDocumento").textContent = perfil.documento;
    $("datoNombre").textContent = `${perfil.nombres} ${perfil.apellidos}`;
    $("email").value = perfil.email;
    $("telefono").value = perfil.telefono ?? "";
}

async function guardarPerfil(evento) {
    evento.preventDefault();
    const boton = $("btnGuardarPerfil");
    boton.disabled = true;
    mostrarMensaje($("mensaje"), "");
    try {
        const telefono = $("telefono").value.trim();
        await api.put("/perfil", { email: $("email").value.trim(), telefono: telefono === "" ? null : telefono });
        mostrarMensaje($("mensaje"), "Datos actualizados", "ok");
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    } finally {
        boton.disabled = false;
    }
}

async function cambiarClave(evento) {
    evento.preventDefault();
    const nueva = $("claveNueva").value;
    if (nueva.length < 8) return mostrarMensaje($("mensaje"), "La nueva contraseña debe tener al menos 8 caracteres");
    if (nueva !== $("claveConfirmar").value) return mostrarMensaje($("mensaje"), "Las contraseñas nuevas no coinciden");

    const boton = $("btnCambiarClave");
    boton.disabled = true;
    mostrarMensaje($("mensaje"), "");
    try {
        // El servidor responde con una sesion nueva (token nuevo): las demas sesiones quedan cerradas
        const sesion = await api.put("/perfil/clave", { claveActual: $("claveActual").value, claveNueva: nueva });
        guardarSesion(sesion);
        $("frmClave").reset();
        $("avisoClave").hidden = true;
        mostrarMensaje($("mensaje"), "Contraseña cambiada. Las demás sesiones se cerraron.", "ok");
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    } finally {
        boton.disabled = false;
    }
}

async function iniciar() {
    const sesion = requireSesion();
    if (!sesion) return;

    $("btnSalir").addEventListener("click", logout);
    $("avisoClave").hidden = !sesion.debeCambiarClave;
    $("frmPerfil").addEventListener("submit", guardarPerfil);
    $("frmClave").addEventListener("submit", cambiarClave);

    try {
        await cargarPerfil();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

iniciar();
