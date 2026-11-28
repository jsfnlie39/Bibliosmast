import { api, textoError } from "./api.js";
import { requireSesion } from "./auth.js";
import { el, vaciar, mostrarMensaje } from "./dom.js";
import { formatoMoneda } from "./multas-formato.js";
import { textoCupo } from "./ficha-formato.js";
import { etiquetaEstadoReserva, claseEstadoReserva, textoTurno } from "./reservas-formato.js";
import { resumenTipo } from "./tipos-formato.js";

const $ = (id) => document.getElementById(id);
const ROLES_PERSONAL = ["ADMIN", "BIBLIOTECARIO"];
const MOTIVOS = { RETRASO: "Retraso", DANO: "Daño", PERDIDA: "Pérdida" };

/** "2026-10-22" -> "22/10/2026" (sin pasar por Date, para evitar corrimientos de zona horaria). */
function formatearFecha(texto) {
    if (!texto) return "—";
    const [anio, mes, dia] = texto.slice(0, 10).split("-");
    return `${dia}/${mes}/${anio}`;
}

const formatearFechaHora = (iso) => (iso ? new Date(iso).toLocaleString("es-CO", { dateStyle: "short", timeStyle: "short" }) : "—");

function filaVacia(columnas, texto) {
    return el("tr", {}, el("td", { colspan: columnas, class: "vacio" }, texto));
}

function etiquetaPrestamo(p) {
    if (p.estado === "VENCIDO") return `Vencido (${p.diasRetraso} d)`;
    return p.estado === "FINALIZADO" ? "Finalizado" : "Activo";
}

function pintar(ficha) {
    const u = ficha.usuario;
    const nombre = `${u.nombres} ${u.apellidos}`;
    document.title = `Biblosmast | ${nombre}`;
    $("titulo").textContent = u.eliminado ? `${nombre} (dado de baja)` : nombre;
    $("dDocumento").textContent = u.documento;
    $("dEmail").textContent = u.email;
    $("dTelefono").textContent = u.telefono ?? "—";
    $("dRol").textContent = u.rol ?? "—";
    $("dEstado").textContent = u.eliminado ? `Baja el ${formatearFecha(u.fechaBaja)}` : u.estado;
    $("dRegistro").textContent = formatearFechaHora(u.fechaRegistro);

    $("dTipo").textContent = `${u.tipoUsuario} (${resumenTipo(ficha)})`;
    $("dCupo").textContent = textoCupo(ficha);
    $("dMultas").textContent = ficha.multasPendientes === 0
        ? "Ninguna"
        : `${ficha.multasPendientes} (${formatoMoneda.format(ficha.valorMultasPendientes)})`;

    const prestamos = $("tbodyPrestamos");
    vaciar(prestamos);
    if (ficha.prestamos.length === 0) prestamos.append(filaVacia(5, "Sin préstamos"));
    for (const p of ficha.prestamos) {
        prestamos.append(el("tr", {},
            el("td", {}, formatearFechaHora(p.fechaPrestamo)),
            el("td", {}, formatearFecha(p.fechaLimite)),
            el("td", {}, etiquetaPrestamo(p)),
            el("td", {}, p.renovaciones),
            el("td", {}, p.detalles.map((d) => el("div", { class: "detalle-item" },
                el("span", {}, `${d.codigo} · ${d.titulo}`),
                el("small", {}, d.fechaDevolucion ? "— devuelto" : "— sin devolver"),
            ))),
        ));
    }

    const reservas = $("tbodyReservas");
    vaciar(reservas);
    if (ficha.reservas.length === 0) reservas.append(filaVacia(5, "Sin reservas"));
    for (const r of ficha.reservas) {
        reservas.append(el("tr", {},
            el("td", {}, el("a", { class: "ficha-vinculo", href: `ficha-libro.html?id=${r.idLibro}` }, r.titulo)),
            el("td", {}, formatearFechaHora(r.fechaReserva)),
            el("td", {}, formatearFecha(r.fechaVencimiento)),
            el("td", {}, el("span", { class: `etiqueta ${claseEstadoReserva(r.estado)}` }, etiquetaEstadoReserva(r.estado))),
            el("td", {}, textoTurno(r)),
        ));
    }

    const multas = $("tbodyMultas");
    vaciar(multas);
    if (ficha.multas.length === 0) multas.append(filaVacia(4, "Sin multas"));
    for (const m of ficha.multas) {
        multas.append(el("tr", {},
            el("td", {}, formatearFechaHora(m.fecha)),
            el("td", {}, MOTIVOS[m.motivo] ?? m.motivo),
            el("td", {}, formatoMoneda.format(m.valor)),
            el("td", {}, m.estado === "PAGADA" ? "Pagada" : "Pendiente"),
        ));
    }
}

async function iniciar() {
    const sesion = requireSesion();
    if (!sesion) return;
    // La ficha es del personal; solo el ADMIN llega desde Usuarios, y el bibliotecario desde Préstamos
    if (!ROLES_PERSONAL.includes(sesion.rol)) {
        window.location.replace("menu.html");
        return;
    }
    $("volver").href = sesion.rol === "ADMIN" ? "usuarios.html" : "prestamos.html";
    const id = Number(new URLSearchParams(window.location.search).get("id"));
    if (!Number.isInteger(id) || id <= 0) {
        mostrarMensaje($("mensaje"), "Falta indicar el usuario: abre la ficha desde la lista de usuarios o de préstamos");
        return;
    }
    try {
        pintar(await api.get(`/usuarios/${id}/historial`));
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    }
}

iniciar();
