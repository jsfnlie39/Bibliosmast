/** Crea un elemento DOM sin usar innerHTML (evita XSS). */
export function el(etiqueta, props = {}, ...hijos) {
    const nodo = document.createElement(etiqueta);
    for (const [clave, valor] of Object.entries(props)) {
        if (valor === false || valor == null) continue;
        if (clave === "class") nodo.className = valor;
        else if (clave.startsWith("on") && typeof valor === "function") {
            nodo.addEventListener(clave.slice(2).toLowerCase(), valor);
        } else nodo.setAttribute(clave, valor === true ? "" : valor);
    }
    for (const hijo of hijos.flat()) {
        nodo.append(hijo instanceof Node ? hijo : document.createTextNode(hijo ?? ""));
    }
    return nodo;
}

export function vaciar(nodo) {
    nodo.replaceChildren();
}

export function mostrarMensaje(nodo, texto, tipo = "error") {
    nodo.textContent = texto;
    nodo.className = `mensaje ${tipo}`;
    nodo.hidden = !texto;
}

/** Llena un <select> con [{id, nombre}]. */
export function llenarSelect(select, opciones, textoVacio = null) {
    vaciar(select);
    if (textoVacio) select.append(el("option", { value: "" }, textoVacio));
    for (const o of opciones) select.append(el("option", { value: o.id }, o.nombre));
}
