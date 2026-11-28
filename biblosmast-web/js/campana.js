// La campana de la barra superior: avisos de vencimiento y reservas listas de la persona con sesión.
import { api } from "./api.js";
import { el, vaciar } from "./dom.js";
import { descripcionCampana, etiquetaTipoAviso, textoInsignia, tiempoRelativo } from "./notificaciones-formato.js";

/**
 * Monta la campana dentro de `contenedor`, antes de `antesDe`. Todo falla en silencio: sin avisos la página sigue igual.
 * No usa temporizadores: el número se actualiza al cargar la página, al volver a la pestaña y al abrir la campana.
 */
export function montarCampana(contenedor, antesDe) {
    const insignia = el("span", { class: "campana-insignia", hidden: true });
    const boton = el("button", { class: "campana", type: "button", "aria-haspopup": "true", "aria-expanded": "false" },
        el("span", { class: "campana-icono", "aria-hidden": "true" }, "🔔"), insignia);
    const lista = el("ul", { class: "campana-lista" });
    const leerTodas = el("button", { class: "btn secundario chico", type: "button" }, "Marcar todas como leídas");
    const panel = el("div", { class: "campana-panel", hidden: true, role: "region", "aria-label": "Avisos" },
        el("div", { class: "campana-cabecera" }, el("strong", {}, "Avisos"), leerTodas), lista);
    const envoltura = el("div", { class: "campana-caja" }, boton, panel);
    contenedor.insertBefore(envoltura, antesDe ?? null);

    const pintarCantidad = (cantidad) => {
        const texto = textoInsignia(cantidad);
        insignia.textContent = texto;
        insignia.hidden = texto === "";
        boton.setAttribute("aria-label", descripcionCampana(cantidad));
        boton.title = boton.getAttribute("aria-label");
    };

    async function actualizarCantidad() {
        try {
            pintarCantidad((await api.get("/notificaciones/no-leidas")).noLeidas);
        } catch {
            // Sin sesión o sin conexión: la campana queda sin número
        }
    }

    async function cargarLista() {
        try {
            const avisos = await api.get("/notificaciones");
            vaciar(lista);
            if (avisos.length === 0) {
                lista.append(el("li", { class: "campana-vacia" }, "No tienes avisos"));
            }
            for (const aviso of avisos) {
                const item = el("li", { class: `campana-aviso${aviso.leida ? "" : " sin-leer"}` },
                    el("div", { class: "campana-tipo" }, `${etiquetaTipoAviso(aviso.tipo)} · ${tiempoRelativo(aviso.fechaCreacion)}`),
                    el("div", {}, aviso.mensaje));
                if (!aviso.leida) {
                    item.tabIndex = 0;
                    item.title = "Marcar como leído";
                    const marcar = async () => {
                        try {
                            await api.put(`/notificaciones/${aviso.idNotificacion}/leida`);
                            item.classList.remove("sin-leer");
                            await actualizarCantidad();
                        } catch {
                            // se reintenta en el siguiente clic
                        }
                    };
                    item.addEventListener("click", marcar);
                    item.addEventListener("keydown", (e) => { if (e.key === "Enter") marcar(); });
                }
                lista.append(item);
            }
        } catch {
            vaciar(lista);
            lista.append(el("li", { class: "campana-vacia" }, "No se pudieron cargar los avisos"));
        }
    }

    const abrir = async (abierto) => {
        panel.hidden = !abierto;
        boton.setAttribute("aria-expanded", String(abierto));
        if (abierto) await cargarLista();
    };

    boton.addEventListener("click", () => abrir(panel.hidden));
    leerTodas.addEventListener("click", async () => {
        try {
            await api.put("/notificaciones/leer-todas");
            await Promise.all([cargarLista(), actualizarCantidad()]);
        } catch {
            // se reintenta en el siguiente clic
        }
    });
    document.addEventListener("click", (e) => { if (!envoltura.contains(e.target)) abrir(false); });
    document.addEventListener("keydown", (e) => { if (e.key === "Escape" && !panel.hidden) abrir(false); });
    window.addEventListener("focus", actualizarCantidad);

    actualizarCantidad();
    return { actualizar: actualizarCantidad };
}
