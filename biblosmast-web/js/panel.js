// Panel de inicio: indicadores (KPI) y gráficos a partir de los datos que la API ya expone.
import { api } from "./api.js";
import { ejemplaresPorEstado, librosPorCategoria, prestamosPorDia, prestamosPorEstado, resumen } from "./estadisticas.js";
import { icono } from "./iconos.js";

const PERSONAL = ["ADMIN", "BIBLIOTECARIO"];
const COLORES = {
    azul: "#2457c5", verde: "#1b8449", ambar: "#e0a100", rojo: "#c62f2f", gris: "#8a97a8", violeta: "#7a5af8", turquesa: "#0e9bb3",
};
const COLOR_EJEMPLAR = { DISPONIBLE: COLORES.verde, PRESTADO: COLORES.azul, RESERVADO: COLORES.ambar, DANADO: COLORES.rojo, PERDIDO: COLORES.gris };
const COLOR_PRESTAMO = { ACTIVO: COLORES.azul, VENCIDO: COLORES.rojo, FINALIZADO: COLORES.gris };
const PALETA = [COLORES.azul, COLORES.turquesa, COLORES.violeta, COLORES.verde, COLORES.ambar, COLORES.rojo, COLORES.gris];

let graficos = [];

const pedir = (ruta) => api.get(ruta).catch(() => null); // un dato que el rol no puede ver (403) simplemente no se muestra

function tarjetaKpi({ icono: nombre, valor, sufijo = "", etiqueta, clase = "" }) {
    const tarjeta = document.createElement("div");
    tarjeta.className = `kpi ${clase}`.trim();
    const caja = document.createElement("div");
    caja.className = "kpi-icono";
    caja.insertAdjacentHTML("beforeend", icono(nombre));
    const texto = document.createElement("div");
    const v = document.createElement("div");
    v.className = "kpi-valor";
    v.textContent = String(valor);
    if (sufijo) {
        const s = document.createElement("small");
        s.textContent = ` ${sufijo}`;
        v.append(s);
    }
    const e = document.createElement("div");
    e.className = "kpi-etiqueta";
    e.textContent = etiqueta;
    texto.append(v, e);
    tarjeta.append(caja, texto);
    return tarjeta;
}

function pintarKpis(r) {
    const contenedor = document.getElementById("kpis");
    contenedor.replaceChildren();
    const tarjetas = [];
    if (r.libros !== null) tarjetas.push({ icono: "libros", valor: r.libros, etiqueta: "Libros en el catálogo" });
    if (r.ejemplaresTotal !== null) {
        tarjetas.push({ icono: "ejemplares", valor: r.ejemplaresDisponibles, sufijo: `de ${r.ejemplaresTotal}`, etiqueta: "Ejemplares disponibles", clase: "exito" });
    }
    if (r.prestamosActivos !== null) tarjetas.push({ icono: "prestamos", valor: r.prestamosActivos, etiqueta: "Préstamos activos" });
    if (r.prestamosVencidos !== null) {
        tarjetas.push({ icono: "alerta", valor: r.prestamosVencidos, etiqueta: "Préstamos vencidos", clase: r.prestamosVencidos > 0 ? "alerta" : "" });
    }
    if (r.reservasPendientes !== null) tarjetas.push({ icono: "reservas", valor: r.reservasPendientes, etiqueta: "Reservas pendientes", clase: "aviso" });
    if (r.usuarios !== null) tarjetas.push({ icono: "usuarios", valor: r.usuarios, etiqueta: "Usuarios registrados" });
    for (const t of tarjetas) contenedor.append(tarjetaKpi(t));
}

function mensajeEnCaja(idCanvas, texto) {
    const caja = document.getElementById(idCanvas)?.parentElement;
    if (!caja) return;
    caja.replaceChildren();
    const aviso = document.createElement("div");
    aviso.className = "sin-datos";
    aviso.textContent = texto;
    caja.append(aviso);
}

function estilo(Chart) {
    const css = getComputedStyle(document.documentElement);
    Chart.defaults.color = css.getPropertyValue("--texto-suave").trim() || "#5a6a7e";
    Chart.defaults.borderColor = css.getPropertyValue("--borde").trim() || "#e1e7f0";
    Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
}

function dibujarGraficos(Chart, datos) {
    for (const g of graficos) g.destroy();
    graficos = [];
    estilo(Chart);
    const base = { responsive: true, maintainAspectRatio: false };

    const dona = (id, serie, colores) => {
        const lienzo = document.getElementById(id);
        if (!lienzo) return;
        if (!serie || serie.valores.length === 0) return mensajeEnCaja(id, "Aún no hay datos para mostrar");
        graficos.push(new Chart(lienzo, {
            type: "doughnut",
            data: { labels: serie.etiquetas, datasets: [{ data: serie.valores, backgroundColor: serie.claves.map((c) => colores[c]), borderWidth: 0 }] },
            options: { ...base, cutout: "62%", plugins: { legend: { position: "bottom" } } },
        }));
    };

    if (datos.ejemplares) dona("grafEjemplares", ejemplaresPorEstado(datos.ejemplares), COLOR_EJEMPLAR);
    if (datos.prestamos) dona("grafPrestamos", prestamosPorEstado(datos.prestamos), COLOR_PRESTAMO);

    if (datos.libros) {
        const serie = librosPorCategoria(datos.libros);
        const lienzo = document.getElementById("grafCategorias");
        if (serie.valores.length === 0) mensajeEnCaja("grafCategorias", "Aún no hay libros registrados");
        else {
            graficos.push(new Chart(lienzo, {
                type: "bar",
                data: { labels: serie.etiquetas, datasets: [{ label: "Libros", data: serie.valores, backgroundColor: serie.etiquetas.map((_, i) => PALETA[i % PALETA.length]), borderRadius: 6 }] },
                options: { ...base, indexAxis: "y", plugins: { legend: { display: false } }, scales: { x: { beginAtZero: true, ticks: { precision: 0 } } } },
            }));
        }
    }

    if (datos.prestamos) {
        const serie = prestamosPorDia(datos.prestamos, 14);
        graficos.push(new Chart(document.getElementById("grafActividad"), {
            type: "line",
            data: { labels: serie.etiquetas, datasets: [{ label: "Préstamos", data: serie.valores, borderColor: COLORES.azul, backgroundColor: "rgba(36, 87, 197, .15)", fill: true, tension: 0.3, pointRadius: 3 }] },
            options: { ...base, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } },
        }));
    }
}

/** Pide los datos, pinta los indicadores y dibuja los gráficos. Los gráficos necesitan `npm run dev` (Chart.js). */
export async function cargarPanel(sesion) {
    const personal = PERSONAL.includes(sesion.rol);
    const [libros, ejemplares, prestamos, reservas, usuarios] = await Promise.all([
        pedir("/libros"),
        personal ? pedir("/ejemplares") : null,
        personal ? pedir("/prestamos") : null,
        personal ? pedir("/reservas") : null,
        sesion.rol === "ADMIN" ? pedir("/usuarios") : null,
    ]);
    const datos = { libros, ejemplares, prestamos, reservas, usuarios };
    pintarKpis(resumen(datos));

    // Los gráficos que el rol no puede ver se ocultan
    const visibles = { grafEjemplares: !!ejemplares, grafPrestamos: !!prestamos, grafActividad: !!prestamos, grafCategorias: !!libros };
    for (const [id, visible] of Object.entries(visibles)) {
        const tarjeta = document.getElementById(id)?.closest(".grafico-tarjeta");
        if (tarjeta) tarjeta.hidden = !visible;
    }

    let Chart;
    try {
        ({ default: Chart } = await import("chart.js/auto"));
    } catch {
        for (const id of Object.keys(visibles)) mensajeEnCaja(id, "Para ver los gráficos ejecuta la aplicación con «npm run dev».");
        return;
    }
    dibujarGraficos(Chart, datos);
    window.addEventListener("biblosmast:tema", () => dibujarGraficos(Chart, datos)); // recolorea al cambiar el tema
}
