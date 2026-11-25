import { api, textoError } from "./api.js";
import { requireSesion, logout } from "./auth.js";
import { el, vaciar, mostrarMensaje, llenarSelect } from "./dom.js";

const $ = (id) => document.getElementById(id);

const DIAS_POR_DEFECTO = 7;
const LIMITE = 200;
const ETIQUETAS = {
    LOGIN: "Inicio de sesión", LOGIN_FALLIDO: "Inicio de sesión fallido", LOGIN_BLOQUEADO: "Cuenta bloqueada",
    LOGOUT: "Cierre de sesión", CLAVE_CAMBIADA: "Contraseña cambiada", CLAVE_RESTABLECIDA: "Contraseña restablecida",
    CREAR: "Crear", ACTUALIZAR: "Actualizar", ELIMINAR: "Eliminar", DEVOLUCION: "Devolución",
};
const CLASES = {
    LOGIN: "activo", LOGIN_FALLIDO: "vencido", LOGIN_BLOQUEADO: "vencido", LOGOUT: "finalizado",
    CLAVE_CAMBIADA: "activo", CLAVE_RESTABLECIDA: "vencido", CREAR: "activo", ACTUALIZAR: "finalizado",
    ELIMINAR: "vencido", DEVOLUCION: "finalizado",
};

/** Date -> "YYYY-MM-DD" en hora local. */
function aTextoFecha(fecha) {
    return fecha.toLocaleDateString("en-CA");
}

function formatearFechaHora(iso) {
    return new Date(iso).toLocaleString("es-CO", { dateStyle: "short", timeStyle: "medium" });
}

async function buscar(evento) {
    if (evento) evento.preventDefault();
    const boton = $("btnBuscar");
    boton.disabled = true;
    mostrarMensaje($("mensaje"), "");
    try {
        const parametros = new URLSearchParams({ limite: String(LIMITE) });
        if ($("filtroDesde").value) parametros.set("desde", $("filtroDesde").value);
        if ($("filtroHasta").value) parametros.set("hasta", $("filtroHasta").value);
        if ($("filtroAccion").value) parametros.set("accion", $("filtroAccion").value);
        if ($("filtroUsuario").value) parametros.set("idUsuario", $("filtroUsuario").value);
        pintarTabla(await api.get(`/auditoria?${parametros.toString()}`));
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    } finally {
        boton.disabled = false;
    }
}

function pintarTabla(registros) {
    const cuerpo = $("tbodyAuditoria");
    vaciar(cuerpo);

    $("resumen").textContent = registros.length === LIMITE
        ? `Se muestran los ${LIMITE} registros más recientes. Acota las fechas para ver otros.`
        : `${registros.length} registro(s).`;

    if (registros.length === 0) {
        cuerpo.append(el("tr", {}, el("td", { colspan: 6, class: "vacio" }, "No hay registros para ese filtro")));
        return;
    }
    for (const r of registros) {
        cuerpo.append(el("tr", {},
            el("td", {}, formatearFechaHora(r.fecha)),
            el("td", {}, r.usuario ?? "— desconocido —"),
            el("td", {}, el("span", { class: `etiqueta ${CLASES[r.accion] ?? "finalizado"}` }, ETIQUETAS[r.accion] ?? r.accion)),
            el("td", {}, r.tabla ?? "—"),
            el("td", {}, r.idRegistro ?? "—"),
            el("td", {}, r.detalle ?? "—"),
        ));
    }
}

async function iniciar() {
    const sesion = requireSesion();
    if (!sesion) return;

    // Solo el administrador entra a este modulo.
    if (sesion.rol !== "ADMIN") {
        window.location.replace("menu.html");
        return;
    }

    $("usuarioActual").textContent = `${sesion.nombres} ${sesion.apellidos} · ${sesion.rol}`;
    $("btnSalir").addEventListener("click", logout);
    $("frmFiltros").addEventListener("submit", buscar);

    const hoy = new Date();
    const inicio = new Date();
    inicio.setDate(hoy.getDate() - DIAS_POR_DEFECTO);
    $("filtroHasta").value = aTextoFecha(hoy);
    $("filtroDesde").value = aTextoFecha(inicio);

    try {
        llenarSelect($("filtroUsuario"), await api.get("/usuarios/activos"), "Todos los usuarios");
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
    await buscar();
}

iniciar();
