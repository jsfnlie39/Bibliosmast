import { test } from "node:test";
import assert from "node:assert/strict";
import { contar, ejemplaresPorEstado, librosPorCategoria, prestamosPorDia, prestamosPorEstado, resumen } from "../js/estadisticas.js";

test("contar por campo y por función", () => {
    assert.deepEqual(contar([{ e: "A" }, { e: "B" }, { e: "A" }], "e"), { A: 2, B: 1 });
    assert.deepEqual(contar([1, 2, 3, 4], (n) => (n % 2 ? "impar" : "par")), { impar: 2, par: 2 });
});

test("ejemplares por estado: orden fijo, sin los estados en cero y con tilde", () => {
    const r = ejemplaresPorEstado([{ estado: "PRESTADO" }, { estado: "DISPONIBLE" }, { estado: "DISPONIBLE" }, { estado: "DANADO" }]);
    assert.deepEqual(r.claves, ["DISPONIBLE", "PRESTADO", "DANADO"]);
    assert.deepEqual(r.etiquetas, ["Disponible", "Prestado", "Dañado"]);
    assert.deepEqual(r.valores, [2, 1, 1]);
});

test("préstamos por estado", () => {
    const r = prestamosPorEstado([{ estado: "ACTIVO" }, { estado: "VENCIDO" }, { estado: "VENCIDO" }]);
    assert.deepEqual(r.etiquetas, ["Activos", "Vencidos"]);
    assert.deepEqual(r.valores, [1, 2]);
    assert.deepEqual(prestamosPorEstado([]).valores, []);
});

test("libros por categoría: de mayor a menor y el resto en «Otras»", () => {
    const libros = [
        ...Array(3).fill({ categoria: "Novela" }), ...Array(2).fill({ categoria: "Ciencia" }),
        { categoria: "Historia" }, { categoria: "Arte" }, { categoria: "Poesía" },
    ];
    const r = librosPorCategoria(libros, 2);
    assert.deepEqual(r.etiquetas, ["Novela", "Ciencia", "Otras"]);
    assert.deepEqual(r.valores, [3, 2, 3]);
    assert.deepEqual(librosPorCategoria(libros, 10).etiquetas.includes("Otras"), false);
});

test("préstamos por día: 14 días terminando hoy, con ceros y fechas locales", () => {
    const hoy = new Date(2026, 10, 6); // 6 de noviembre de 2026
    const prestamos = [
        { fechaPrestamo: "2026-11-06T09:30:00" }, { fechaPrestamo: "2026-11-06T18:00:00" },
        { fechaPrestamo: "2026-11-04T10:00:00" }, { fechaPrestamo: "2026-10-01T10:00:00" }, // fuera de la ventana
    ];
    const r = prestamosPorDia(prestamos, 14, hoy);
    assert.equal(r.valores.length, 14);
    assert.equal(r.etiquetas.at(-1), "06/11");
    assert.equal(r.etiquetas[0], "24/10");
    assert.equal(r.valores.at(-1), 2);
    assert.equal(r.valores.at(-3), 1);
    assert.equal(r.valores.reduce((a, b) => a + b, 0), 3);
});

test("resumen: indicadores y null cuando el rol no puede ver el dato", () => {
    const r = resumen({
        libros: [{}, {}], ejemplares: [{ estado: "DISPONIBLE" }, { estado: "PRESTADO" }, { estado: "DISPONIBLE" }],
        prestamos: [{ estado: "ACTIVO" }, { estado: "VENCIDO" }, { estado: "FINALIZADO" }],
        reservas: [{ estado: "PENDIENTE" }, { estado: "ATENDIDA" }], usuarios: null,
    });
    assert.deepEqual(r, {
        libros: 2, ejemplaresTotal: 3, ejemplaresDisponibles: 2, prestamosActivos: 1, prestamosVencidos: 1,
        reservasPendientes: 1, usuarios: null,
    });
    const soloLibros = resumen({ libros: [{}], ejemplares: null, prestamos: null, reservas: null, usuarios: null });
    assert.equal(soloLibros.libros, 1);
    assert.equal(soloLibros.ejemplaresTotal, null);
    assert.equal(soloLibros.prestamosVencidos, null);
});

test("resumen: las reservas listas para recoger también cuentan como pendientes", () => {
    const r = resumen({
        libros: null, ejemplares: null, prestamos: null, usuarios: null,
        reservas: [{ estado: "PENDIENTE" }, { estado: "LISTA" }, { estado: "LISTA" }, { estado: "CANCELADA" }, { estado: "VENCIDA" }],
    });
    assert.equal(r.reservasPendientes, 3);
});
