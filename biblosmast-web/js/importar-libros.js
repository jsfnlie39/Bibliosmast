// Diálogo «Importar CSV» de Libros: revisar el archivo, ver los errores línea por línea y guardar todo junto.
import { api, textoError } from "./api.js";
import { el, vaciar, mostrarMensaje } from "./dom.js";
import {
    PLANTILLA_CSV, puedeImportar, resumenImportacion, textoLinea, tieneProblemaDeCodificacion, validarArchivo,
} from "./importacion-formato.js";

const $ = (id) => document.getElementById(id);

/** Conecta el botón y el diálogo. `alImportar` se llama cuando se guardaron libros nuevos (para recargar la lista). */
export function montarImportacion(alImportar) {
    let contenido = "";
    let revision = null;

    function reiniciar() {
        contenido = "";
        revision = null;
        $("archivoCsv").value = "";
        mostrarMensaje($("mensajeImportar"), "");
        mostrarMensaje($("resumenImportar"), "", "ok");
        $("tablaImportar").hidden = true;
        vaciar($("tbodyImportar"));
        $("btnConfirmarImportar").disabled = true;
    }

    function pintar(resultado) {
        revision = resultado;
        const hayErrores = resultado.lineasConErrores > 0;
        mostrarMensaje($("resumenImportar"), resumenImportacion(resultado), hayErrores ? "error" : "ok");
        const cuerpo = $("tbodyImportar");
        vaciar(cuerpo);
        for (const l of resultado.lineas) {
            cuerpo.append(el("tr", { class: l.errores.length ? "fila-error" : "" },
                el("td", {}, l.numeroLinea), el("td", {}, l.isbn || "—"), el("td", {}, l.titulo || "—"), el("td", {}, textoLinea(l))));
        }
        $("tablaImportar").hidden = false;
        $("btnConfirmarImportar").disabled = !puedeImportar(resultado);
        $("btnConfirmarImportar").textContent = puedeImportar(resultado)
            ? `Importar ${resultado.lineasValidas} ${resultado.lineasValidas === 1 ? "libro" : "libros"}` : "Importar";
    }

    async function leerArchivo() {
        const archivo = $("archivoCsv").files[0];
        const problema = validarArchivo(archivo);
        if (problema) throw new Error(problema);
        const texto = await archivo.text();
        if (tieneProblemaDeCodificacion(texto)) {
            throw new Error("El archivo no está en UTF-8 (las tildes se dañarían). En Excel guárdalo como «CSV UTF-8 (delimitado por comas)»");
        }
        return texto;
    }

    async function ejecutar(soloValidar) {
        mostrarMensaje($("mensajeImportar"), "");
        const botones = [$("btnRevisar"), $("btnConfirmarImportar")];
        botones.forEach((b) => { b.disabled = true; });
        try {
            if (soloValidar) contenido = await leerArchivo();
            const resultado = await api.post("/libros/importar", { contenido, soloValidar });
            pintar(resultado);
            if (resultado.importado) {
                $("btnConfirmarImportar").disabled = true;
                await alImportar(resultado);
            }
        } catch (error) {
            mostrarMensaje($("mensajeImportar"), error instanceof Error && !error.status ? error.message : textoError(error));
        } finally {
            $("btnRevisar").disabled = false;
            if (!revision?.importado) $("btnConfirmarImportar").disabled = !puedeImportar(revision);
        }
    }

    $("btnImportar").hidden = false;
    $("btnImportar").addEventListener("click", () => { reiniciar(); $("dlgImportar").showModal(); });
    $("btnCerrarImportar").addEventListener("click", () => $("dlgImportar").close());
    $("btnRevisar").addEventListener("click", () => ejecutar(true));
    $("btnConfirmarImportar").addEventListener("click", () => ejecutar(false));
    // Si cambia el archivo, la revisión anterior ya no vale
    $("archivoCsv").addEventListener("change", () => {
        revision = null;
        $("btnConfirmarImportar").disabled = true;
        $("tablaImportar").hidden = true;
        mostrarMensaje($("resumenImportar"), "", "ok");
    });
    $("btnPlantilla").addEventListener("click", () => {
        const enlace = el("a", {
            href: URL.createObjectURL(new Blob([PLANTILLA_CSV], { type: "text/csv;charset=utf-8" })),
            download: "plantilla-libros.csv",
        });
        document.body.append(enlace);
        enlace.click();
        enlace.remove();
        URL.revokeObjectURL(enlace.href);
    });
}
