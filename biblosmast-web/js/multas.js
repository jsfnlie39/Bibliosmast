import { api, textoError } from "./api.js";
import { requireSesion, logout } from "./auth.js";
import { el, vaciar, mostrarMensaje, llenarSelect } from "./dom.js";
import { formatoMoneda } from "./multas-formato.js";

const $ = (id) => document.getElementById(id);

const ROLES_PERSONAL = ["ADMIN", "BIBLIOTECARIO"];
const MOTIVOS = { RETRASO: "Retraso", DANO: "Daño", PERDIDA: "Pérdida" };

let multas = [];
let sesion = null;

// ---------------- utilidades ----------------

function formatearFechaHora(iso) {
    return new Date(iso).toLocaleString("es-CO", { dateStyle: "short", timeStyle: "short" });
}

// ---------------- valores de las multas (solo ADMIN) ----------------

async function cargarValores() {
    // La configuración también guarda las reglas de renovación; aquí solo van los valores de las multas
    const valores = (await api.get("/configuracion")).filter((v) => v.clave.startsWith("multa_"));
    const contenedor = $("camposValores");
    vaciar(contenedor);

    for (const v of valores) {
        const id = `cfg_${v.clave}`;
        const entrada = el("input", { id, type: "number", min: 0, max: 1000000, step: 100, value: v.valor });
        const boton = el("button", {
            class: "btn chico", type: "button",
            onclick: () => guardarValor(v.clave, entrada, boton),
        }, "Guardar");
        contenedor.append(el("div", { class: "campo-valor" },
            el("label", { for: id }, v.descripcion),
            el("div", { class: "campo-valor-fila" }, entrada, boton),
        ));
    }
}

async function guardarValor(clave, entrada, boton) {
    if (entrada.value === "") {
        mostrarMensaje($("mensaje"), "Escribe un valor (puede ser 0 para no cobrar ese concepto)");
        return;
    }
    boton.disabled = true;
    try {
        await api.put(`/configuracion/${encodeURIComponent(clave)}`, { valor: Number(entrada.value) });
        mostrarMensaje($("mensaje"), "Valor actualizado. Se aplica a las próximas devoluciones", "ok");
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    } finally {
        boton.disabled = false;
    }
}

// ---------------- datos ----------------

async function cargarUsuarios() {
    const usuarios = await api.get("/usuarios/activos");
    const seleccionado = $("filtroUsuario").value;
    llenarSelect($("filtroUsuario"), usuarios, "Todos los usuarios");
    $("filtroUsuario").value = seleccionado;
}

async function cargarMultas() {
    try {
        const parametros = new URLSearchParams();
        if ($("filtroEstado").value) parametros.set("estado", $("filtroEstado").value);
        if ($("filtroUsuario").value) parametros.set("idUsuario", $("filtroUsuario").value);
        const consulta = parametros.toString();
        multas = await api.get(consulta ? `/multas?${consulta}` : "/multas");
        pintarTabla();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

// ---------------- tabla ----------------

function pintarTabla() {
    const cuerpo = $("tbodyMultas");
    vaciar(cuerpo);

    const pendiente = multas.filter((m) => m.estado === "PENDIENTE").reduce((suma, m) => suma + m.valor, 0);
    $("totalPendiente").textContent = formatoMoneda.format(pendiente);

    if (multas.length === 0) {
        cuerpo.append(el("tr", {}, el("td", { colspan: 8, class: "vacio" }, "No hay multas para mostrar")));
        return;
    }

    for (const m of multas) {
        const pagada = m.estado === "PAGADA";
        const acciones = el("td", { class: "acciones" });
        if (!pagada) {
            acciones.append(
                el("button", { class: "btn secundario chico", type: "button", onclick: () => pagar(m) }, "Registrar pago"),
            );
            // Anular (borrar) una multa es solo del administrador, como el resto de eliminaciones
            if (sesion.rol === "ADMIN") {
                acciones.append(el("button", { class: "btn peligro chico", type: "button", onclick: () => anular(m) }, "Anular"));
            }
        }
        cuerpo.append(el("tr", {},
            el("td", {}, m.idMulta),
            el("td", {}, `${m.usuario} (${m.documento})`),
            el("td", {}, `N° ${m.idPrestamo}`),
            el("td", {}, MOTIVOS[m.motivo] ?? m.motivo),
            el("td", {}, formatoMoneda.format(m.valor)),
            el("td", {}, el("span", { class: `etiqueta ${pagada ? "activo" : "vencido"}` }, pagada ? "Pagada" : "Pendiente")),
            el("td", {}, pagada ? `Pagada ${formatearFechaHora(m.fechaPago)}` : formatearFechaHora(m.fecha)),
            acciones,
        ));
    }
}

// ---------------- acciones ----------------

async function pagar(multa) {
    if (!window.confirm(`¿Registrar el pago de ${formatoMoneda.format(multa.valor)} de ${multa.usuario}?`)) return;
    try {
        await api.put(`/multas/${multa.idMulta}/pago`, {});
        mostrarMensaje($("mensaje"), "Pago registrado", "ok");
        await cargarMultas();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

async function anular(multa) {
    if (!window.confirm(`¿Anular la multa de ${formatoMoneda.format(multa.valor)} de ${multa.usuario}? Se elimina y no se puede deshacer.`)) return;
    try {
        await api.delete(`/multas/${multa.idMulta}`);
        mostrarMensaje($("mensaje"), "Multa anulada", "ok");
        await cargarMultas();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
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
    $("filtroEstado").addEventListener("change", cargarMultas);
    $("filtroUsuario").addEventListener("change", cargarMultas);

    // Solo el administrador cambia los valores de las multas.
    if (sesion.rol === "ADMIN") {
        $("panelValores").hidden = false;
        try {
            await cargarValores();
        } catch (error) {
            mostrarMensaje($("mensaje"), textoError(error));
        }
    }

    try {
        await cargarUsuarios();
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
    await cargarMultas();
}

iniciar();
