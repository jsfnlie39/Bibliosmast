import { api, textoError } from "./api.js";
import { requireSesion } from "./auth.js";
import { el, vaciar, mostrarMensaje } from "./dom.js";
import { formatoMoneda } from "./multas-formato.js";
import { parametrosReporte, serieLibros, serieMeses, serieRetrasos } from "./reportes-formato.js";

const $ = (id) => document.getElementById(id);
const ROLES_PERSONAL = ["ADMIN", "BIBLIOTECARIO"];
const AZUL = "#2457c5";
const AMBAR = "#d9822b";

let Chart = null;
let graficos = [];

function filaVacia(columnas, texto) {
    return el("tr", {}, el("td", { colspan: columnas, class: "vacio" }, texto));
}

async function cargarChart() {
    if (Chart) return Chart;
    try {
        ({ default: Chart } = await import("chart.js/auto"));
        const css = getComputedStyle(document.documentElement);
        Chart.defaults.color = css.getPropertyValue("--texto-suave").trim() || "#5a6a7e";
        Chart.defaults.borderColor = css.getPropertyValue("--borde").trim() || "#e1e7f0";
        Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
    } catch {
        Chart = null; // sin gráficos: las tablas siguen funcionando
    }
    return Chart;
}

function sinDatos(idLienzo, texto) {
    const caja = $(idLienzo)?.parentElement;
    if (!caja) return;
    caja.replaceChildren(el("div", { class: "sin-datos" }, texto));
}

function dibujar(lienzo, config, hayDatos, textoVacio) {
    if (!hayDatos) return sinDatos(lienzo, textoVacio);
    if (!Chart) return;
    graficos.push(new Chart($(lienzo), config));
}

function restaurarCajas() {
    // Si un reporte anterior quedó «sin datos», se vuelve a poner el lienzo
    for (const [id, etiqueta] of [["grafMeses", "Gráfico de préstamos por mes"], ["grafLibros", "Gráfico de libros más prestados"], ["grafRetrasos", "Gráfico de usuarios con más retrasos"]]) {
        if ($(id)) continue;
        const caja = document.querySelector(`[data-grafico="${id}"]`);
        if (caja) caja.replaceChildren(el("canvas", { id, role: "img", "aria-label": etiqueta }));
    }
}

function pintarMeses(items) {
    const cuerpo = $("tbodyMeses");
    vaciar(cuerpo);
    if (items.length === 0) cuerpo.append(filaVacia(2, "Sin datos"));
    for (const i of items) cuerpo.append(el("tr", {}, el("td", {}, i.mes), el("td", {}, i.prestamos)));
    const s = serieMeses(items);
    dibujar("grafMeses", {
        type: "bar",
        data: { labels: s.etiquetas, datasets: [{ label: "Préstamos", data: s.valores, backgroundColor: AZUL, borderRadius: 6 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } },
    }, s.valores.some((v) => v > 0), "Aún no hay préstamos en estos meses");
}

function pintarLibros(items) {
    const cuerpo = $("tbodyLibros");
    vaciar(cuerpo);
    if (items.length === 0) cuerpo.append(filaVacia(2, "Sin préstamos en el periodo"));
    for (const i of items) {
        cuerpo.append(el("tr", {},
            el("td", {}, el("a", { class: "ficha-vinculo", href: `ficha-libro.html?id=${i.idLibro}` }, i.titulo)),
            el("td", {}, i.prestamos)));
    }
    const s = serieLibros(items);
    dibujar("grafLibros", {
        type: "bar",
        data: { labels: s.etiquetas, datasets: [{ label: "Veces prestado", data: s.valores, backgroundColor: AZUL, borderRadius: 6 }] },
        options: { responsive: true, maintainAspectRatio: false, indexAxis: "y", plugins: { legend: { display: false } }, scales: { x: { beginAtZero: true, ticks: { precision: 0 } } } },
    }, items.length > 0, "Sin préstamos en el periodo");
}

function pintarRetrasos(items) {
    const cuerpo = $("tbodyRetrasos");
    vaciar(cuerpo);
    if (items.length === 0) cuerpo.append(filaVacia(5, "Nadie tiene retrasos en el periodo"));
    for (const i of items) {
        cuerpo.append(el("tr", {},
            el("td", {}, el("a", { class: "ficha-vinculo", href: `ficha-usuario.html?id=${i.idUsuario}` }, i.usuario)),
            el("td", {}, i.documento),
            el("td", {}, i.multasRetraso),
            el("td", {}, formatoMoneda.format(i.valorMultasRetraso)),
            el("td", {}, i.ejemplaresVencidosAhora)));
    }
    const s = serieRetrasos(items);
    dibujar("grafRetrasos", {
        type: "bar",
        data: {
            labels: s.etiquetas,
            datasets: [
                { label: "Multas por retraso", data: s.multas, backgroundColor: AZUL, borderRadius: 6 },
                { label: "Vencidos ahora", data: s.vencidos, backgroundColor: AMBAR, borderRadius: 6 },
            ],
        },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: "bottom" } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } },
    }, items.length > 0, "Nadie tiene retrasos en el periodo");
}

async function actualizar(evento) {
    evento?.preventDefault();
    const boton = $("btnActualizar");
    boton.disabled = true;
    mostrarMensaje($("mensaje"), "");
    try {
        const periodo = parametrosReporte({ desde: $("desde").value, hasta: $("hasta").value, limite: $("limite").value });
        const [meses, libros, retrasos] = await Promise.all([
            api.get(`/reportes/prestamos-por-mes?meses=${encodeURIComponent($("meses").value)}`),
            api.get(`/reportes/libros-mas-prestados${periodo}`),
            api.get(`/reportes/usuarios-con-retrasos${periodo}`),
            cargarChart(),
        ]);
        for (const g of graficos) g.destroy();
        graficos = [];
        restaurarCajas();
        pintarMeses(meses);
        pintarLibros(libros);
        pintarRetrasos(retrasos);
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    } finally {
        boton.disabled = false;
    }
}

async function iniciar() {
    const sesion = requireSesion();
    if (!sesion) return;
    if (!ROLES_PERSONAL.includes(sesion.rol)) {
        window.location.replace("menu.html");
        return;
    }
    // Cada lienzo vive en su caja: así se puede reemplazar por «sin datos» y volver a ponerlo
    for (const id of ["grafMeses", "grafLibros", "grafRetrasos"]) $(id).parentElement.dataset.grafico = id;
    $("frmReportes").addEventListener("submit", actualizar);
    await actualizar();
}

iniciar();
