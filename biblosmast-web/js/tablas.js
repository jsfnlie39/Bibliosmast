// Mejora cualquier tabla de la aplicación sin tocar el código de cada página:
// ordenar al hacer clic en el encabezado, paginar y descargar como CSV.
// Observa el <tbody>: cuando una página vuelve a dibujar sus filas, la tabla se reordena y pagina sola.

const POR_PAGINA = 10;

/** Convierte el texto de una celda en algo comparable: fecha (dd/mm/aaaa), número o texto. */
export function valorOrden(texto) {
    const t = texto.trim();
    const fecha = /^(\d{2})\/(\d{2})\/(\d{4})/.exec(t);
    if (fecha) return Number(fecha[3] + fecha[2] + fecha[1]);
    if (/^-?\d+([.,]\d+)?$/.test(t)) return Number(t.replace(",", "."));
    const inicio = /^(\d+) de \d+$/.exec(t); // «1 de 2» (disponibles de un libro)
    if (inicio) return Number(inicio[1]);
    return t.toLocaleLowerCase("es");
}

export function comparar(a, b) {
    if (typeof a === "number" && typeof b === "number") return a - b;
    return String(a).localeCompare(String(b), "es", { numeric: true, sensitivity: "base" });
}

/** Una celda de CSV: entre comillas y sin riesgo de que Excel la tome como fórmula. */
export function celdaCsv(texto) {
    let t = texto.replace(/\s+/g, " ").trim();
    if (/^[=+\-@]/.test(t)) t = `'${t}`;
    return `"${t.replace(/"/g, '""')}"`;
}

export function mejorarTabla(tabla, { porPagina = POR_PAGINA, nombreArchivo = "datos" } = {}) {
    const cuerpo = tabla.tBodies[0];
    // Los encabezados se leen cada vez: algunas páginas (catálogos) los dibujan después, con JavaScript
    const cabeceras = () => [...(tabla.tHead?.rows[0]?.cells ?? [])];
    const contenedor = tabla.closest(".tabla-wrap");
    if (!cuerpo || !contenedor) return null;

    const estado = { columna: null, ascendente: true, pagina: 1, firma: "" };
    const pie = document.createElement("div");
    pie.className = "tabla-pie";
    contenedor.after(pie);

    const filasDatos = () => [...cuerpo.rows].filter((f) => !f.querySelector("td.vacio"));
    const textoCelda = (fila, i) => (fila.cells[i]?.textContent ?? "");

    function ordenar(filas) {
        if (estado.columna === null) return filas;
        const i = estado.columna;
        const signo = estado.ascendente ? 1 : -1;
        return [...filas].sort((a, b) => signo * comparar(valorOrden(textoCelda(a, i)), valorOrden(textoCelda(b, i))));
    }

    let observador = null;

    /** Marca como ordenables los encabezados con texto. Si cambian las columnas, se olvida el orden anterior. */
    function prepararCabeceras() {
        const ths = cabeceras();
        const firma = ths.map((th) => th.textContent.trim()).join("|");
        if (firma !== estado.firma) {
            estado.firma = firma;
            estado.columna = null;
            estado.ascendente = true;
            estado.pagina = 1;
        }
        ths.forEach((th) => {
            const ordenable = th.textContent.trim() !== "";
            th.classList.toggle("ordenable", ordenable);
            if (ordenable) th.tabIndex = 0;
        });
    }

    function dibujar() {
        observador?.disconnect(); // reordenar filas también cambia el <tbody>: no hay que reaccionar a eso
        prepararCabeceras();
        const filas = ordenar(filasDatos());
        const total = filas.length;
        const paginas = Math.max(1, Math.ceil(total / porPagina));
        estado.pagina = Math.min(estado.pagina, paginas);
        const desde = (estado.pagina - 1) * porPagina;

        for (const fila of filas) cuerpo.append(fila); // deja las filas en el orden elegido
        filas.forEach((fila, n) => {
            fila.hidden = n < desde || n >= desde + porPagina;
        });
        // la fila «No hay registros» (si existe) va siempre al final
        for (const vacia of [...cuerpo.rows].filter((f) => f.querySelector("td.vacio"))) cuerpo.append(vacia);

        cabeceras().forEach((th, i) => {
            if (th.classList.contains("ordenable")) {
                if (estado.columna === i) th.dataset.orden = estado.ascendente ? "asc" : "desc";
                else delete th.dataset.orden;
                th.setAttribute("aria-sort", estado.columna === i ? (estado.ascendente ? "ascending" : "descending") : "none");
            }
        });

        pintarPie(total, paginas, desde);
        observador?.observe(cuerpo, { childList: true });
    }

    function pintarPie(total, paginas, desde) {
        pie.replaceChildren();
        pie.hidden = total === 0;
        if (total === 0) return;

        const resumen = document.createElement("span");
        const hasta = Math.min(desde + porPagina, total);
        resumen.textContent = `Mostrando ${desde + 1}–${hasta} de ${total}`;

        const nav = document.createElement("div");
        nav.className = "paginas";
        const boton = (texto, pagina, deshabilitado, etiqueta) => {
            const b = document.createElement("button");
            b.type = "button";
            b.className = "btn secundario chico";
            b.textContent = texto;
            b.setAttribute("aria-label", etiqueta);
            b.disabled = deshabilitado;
            b.addEventListener("click", () => {
                estado.pagina = pagina;
                dibujar();
            });
            return b;
        };
        const indicador = document.createElement("span");
        indicador.textContent = `Página ${estado.pagina} de ${paginas}`;
        nav.append(
            boton("‹", estado.pagina - 1, estado.pagina <= 1, "Página anterior"),
            indicador,
            boton("›", estado.pagina + 1, estado.pagina >= paginas, "Página siguiente"),
        );

        const csv = document.createElement("button");
        csv.type = "button";
        csv.className = "btn secundario chico";
        csv.textContent = "Descargar CSV";
        csv.addEventListener("click", () => descargarCsv());

        pie.append(resumen, nav, csv);
    }

    function generarCsv() {
        const columnas = cabeceras().map((th, i) => ({ i, texto: th.textContent.trim() })).filter((c) => c.texto !== "");
        const lineas = [columnas.map((c) => celdaCsv(c.texto)).join(",")];
        for (const fila of ordenar(filasDatos())) {
            lineas.push(columnas.map((c) => celdaCsv(textoCelda(fila, c.i))).join(","));
        }
        return lineas.join("\r\n");
    }

    function descargarCsv() {
        const contenido = "\ufeff" + generarCsv(); // BOM para que Excel respete las tildes
        const url = URL.createObjectURL(new Blob([contenido], { type: "text/csv;charset=utf-8" }));
        const enlace = document.createElement("a");
        enlace.href = url;
        enlace.download = `${nombreArchivo}.csv`;
        document.body.append(enlace);
        enlace.click();
        enlace.remove();
        URL.revokeObjectURL(url);
    }

    // Clic (o Enter/espacio) en un encabezado ordenable: delegado en el <thead>, porque los encabezados pueden cambiar
    const alternar = (th) => {
        const i = cabeceras().indexOf(th);
        if (i < 0 || !th.classList.contains("ordenable")) return;
        if (estado.columna === i) estado.ascendente = !estado.ascendente;
        else {
            estado.columna = i;
            estado.ascendente = true;
        }
        estado.pagina = 1;
        dibujar();
    };
    tabla.tHead?.addEventListener("click", (e) => {
        const th = e.target.closest("th");
        if (th) alternar(th);
    });
    tabla.tHead?.addEventListener("keydown", (e) => {
        const th = e.target.closest?.("th");
        if (th && (e.key === "Enter" || e.key === " ")) {
            e.preventDefault();
            alternar(th);
        }
    });

    observador = new MutationObserver(() => dibujar());
    // si una página reescribe los encabezados (catálogos), también hay que volver a dibujar
    if (tabla.tHead) new MutationObserver(() => dibujar()).observe(tabla.tHead, { childList: true, subtree: true });
    dibujar();
    return { dibujar, generarCsv, estado };
}

export function mejorarTablas() {
    const nombre = document.body.dataset.pagina ?? "datos";
    document.querySelectorAll(".tabla-wrap table").forEach((t) => mejorarTabla(t, { nombreArchivo: nombre }));
}
