import { test } from "node:test";
import assert from "node:assert/strict";

const { PLANTILLA_CSV, validarArchivo, tieneProblemaDeCodificacion, puedeImportar, resumenImportacion, textoLinea } =
  await import("../js/importacion-formato.js");

test("PLANTILLA_CSV: BOM, separador ; y las columnas obligatorias primero", () => {
  assert.equal(PLANTILLA_CSV.charCodeAt(0), 0xFEFF);
  const [encabezado, ejemplo] = PLANTILLA_CSV.slice(1).split("\n");
  assert.deepEqual(encabezado.split(";").slice(0, 5), ["isbn", "titulo", "autores", "categoria", "editorial"]);
  assert.equal(ejemplo.split(";").length, encabezado.split(";").length);
});

test("validarArchivo: extensión, vacío y tamaño", () => {
  assert.equal(validarArchivo(null), "Selecciona un archivo CSV");
  assert.equal(validarArchivo({ name: "libros.xlsx", size: 10 }), "El archivo debe ser un CSV (.csv)");
  assert.equal(validarArchivo({ name: "libros.csv", size: 0 }), "El archivo está vacío");
  assert.equal(validarArchivo({ name: "libros.csv", size: 2_000_000 }), "El archivo es demasiado grande (máximo 1 MB)");
  assert.equal(validarArchivo({ name: "LIBROS.CSV", size: 500 }), "");
});

test("tieneProblemaDeCodificacion: detecta el carácter de reemplazo", () => {
  assert.equal(tieneProblemaDeCodificacion("Cien a\uFFFDos"), true);
  assert.equal(tieneProblemaDeCodificacion("Cien años"), false);
  assert.equal(tieneProblemaDeCodificacion(undefined), false);
});

test("puedeImportar: solo tras una revisión limpia", () => {
  assert.equal(puedeImportar(null), false);
  assert.equal(puedeImportar({ soloValidar: true, lineasConErrores: 0, lineasValidas: 3 }), true);
  assert.equal(puedeImportar({ soloValidar: true, lineasConErrores: 1, lineasValidas: 2 }), false);
  assert.equal(puedeImportar({ soloValidar: true, lineasConErrores: 0, lineasValidas: 0 }), false);
  assert.equal(puedeImportar({ soloValidar: false, importado: true, lineasConErrores: 0, lineasValidas: 3 }), false);
});

test("resumenImportacion: revisión correcta, con errores e importación", () => {
  assert.equal(resumenImportacion({ soloValidar: true, totalLineas: 12, lineasValidas: 12, lineasConErrores: 0 }),
    "Las 12 líneas están correctas. Pulsa «Importar» para guardarlas.");
  assert.equal(resumenImportacion({ soloValidar: true, totalLineas: 12, lineasValidas: 10, lineasConErrores: 2 }),
    "Revisadas 12 líneas: 10 correctas y 2 con errores. No se guardó nada: corrige el archivo y vuelve a revisarlo.");
  assert.equal(resumenImportacion({ importado: true, librosCreados: 1 }), "Se importaron 1 libro.");
  assert.equal(resumenImportacion({ importado: true, librosCreados: 5 }), "Se importaron 5 libros.");
  assert.equal(resumenImportacion(null), "");
});

test("textoLinea: Correcta o los errores unidos", () => {
  assert.equal(textoLinea({ errores: [] }), "Correcta");
  assert.equal(textoLinea({ errores: ["Falta el ISBN", "Falta el título"] }), "Falta el ISBN · Falta el título");
});
