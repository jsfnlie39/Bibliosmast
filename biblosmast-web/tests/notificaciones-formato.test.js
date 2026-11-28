import { test } from "node:test";
import assert from "node:assert/strict";

const { textoInsignia, etiquetaTipoAviso, tiempoRelativo, descripcionCampana } =
  await import("../js/notificaciones-formato.js");

test("textoInsignia: vacía sin avisos y «9+» desde diez", () => {
  assert.equal(textoInsignia(0), "");
  assert.equal(textoInsignia(undefined), "");
  assert.equal(textoInsignia(3), "3");
  assert.equal(textoInsignia(9), "9");
  assert.equal(textoInsignia(10), "9+");
});

test("etiquetaTipoAviso: nombres en español y tipo desconocido tal cual", () => {
  assert.equal(etiquetaTipoAviso("POR_VENCER"), "Por vencer");
  assert.equal(etiquetaTipoAviso("VENCIDO"), "Vencido");
  assert.equal(etiquetaTipoAviso("RESERVA_LISTA"), "Reserva lista");
  assert.equal(etiquetaTipoAviso("OTRO"), "OTRO");
});

test("tiempoRelativo: minutos, horas, ayer y fecha", () => {
  const ahora = new Date("2026-11-24T12:00:00");
  assert.equal(tiempoRelativo("2026-11-24T11:59:40", ahora), "ahora");
  assert.equal(tiempoRelativo("2026-11-24T11:45:00", ahora), "hace 15 min");
  assert.equal(tiempoRelativo("2026-11-24T09:00:00", ahora), "hace 3 h");
  assert.equal(tiempoRelativo("2026-11-23T10:00:00", ahora), "ayer");
  assert.match(tiempoRelativo("2026-11-10T10:00:00", ahora), /^\d{1,2}\/\d{1,2}\/2026$/);
  assert.equal(tiempoRelativo("2026-11-25T10:00:00", ahora), ""); // fecha futura
  assert.equal(tiempoRelativo("no es fecha", ahora), "");
});

test("descripcionCampana: texto para lectores de pantalla", () => {
  assert.equal(descripcionCampana(0), "Avisos: no tienes avisos sin leer");
  assert.equal(descripcionCampana(1), "Avisos: tienes 1 aviso sin leer");
  assert.equal(descripcionCampana(4), "Avisos: tienes 4 avisos sin leer");
});
