import { test } from "node:test";
import assert from "node:assert/strict";

const { formatoMoneda, textoMultasGeneradas } = await import("../js/multas-formato.js");

// Intl separa el símbolo con un espacio especial según la versión de Node: se compara sin espacios
const sinEspacios = (t) => t.replace(/\s/g, "");

test("formatoMoneda: pesos colombianos sin decimales cuando no hacen falta", () => {
  assert.equal(sinEspacios(formatoMoneda.format(10000)), "$10.000");
  assert.equal(sinEspacios(formatoMoneda.format(0)), "$0");
  assert.equal(sinEspacios(formatoMoneda.format(1500.5)), "$1.500,5");
});

test("textoMultasGeneradas: vacío cuando la devolución no generó multas", () => {
  assert.equal(textoMultasGeneradas({ multasGeneradas: 0, valorMultasGeneradas: 0 }), "");
  assert.equal(textoMultasGeneradas({}), "");
  assert.equal(textoMultasGeneradas(null), "");
});

test("textoMultasGeneradas: singular y plural con el total", () => {
  assert.equal(sinEspacios(textoMultasGeneradas({ multasGeneradas: 1, valorMultasGeneradas: 3000 })), "Segeneró1multapor$3.000.");
  assert.equal(
    sinEspacios(textoMultasGeneradas({ multasGeneradas: 2, valorMultasGeneradas: 13000 })),
    "Segeneraron2multasporuntotalde$13.000.",
  );
});
