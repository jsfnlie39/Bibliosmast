package com.biblosmast.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvLectorTest {

    @Test
    void leeExcelEnEspanolConBomYFinDeLineaWindows() {
        var r = CsvLector.leer("\uFEFFisbn;titulo;autores\r\n978-1;Cien años;Gabriel García Márquez\r\n978-2;Otro;A|B\r\n");
        assertEquals(';', r.separador());
        assertEquals(3, r.filas().size());
        assertEquals("isbn", r.filas().get(0).campos().get(0));
        assertEquals("Cien años", r.filas().get(1).campos().get(1));
        assertEquals(3, r.filas().get(2).linea());
    }

    @Test
    void leeComillasEscapadasYSaltosDeLineaDentroDeUnCampo() {
        var r = CsvLector.leer("isbn,titulo,descripcion\n1,\"Hola, mundo\",\"dijo \"\"hola\"\"\ny siguió\"\n2,Dos,fin\n");
        assertEquals(',', r.separador());
        assertEquals("Hola, mundo", r.filas().get(1).campos().get(1));
        assertEquals("dijo \"hola\"\ny siguió", r.filas().get(1).campos().get(2));
        assertEquals(4, r.filas().get(2).linea());
    }

    @Test
    void detectaTabuladorEIgnoraLineasVacias() {
        assertEquals('\t', CsvLector.leer("a\tb\tc\n1\t2\t3").separador());
        var r = CsvLector.leer("a;b\n\n;\n1;\n \n");
        assertEquals(2, r.filas().size());
        assertEquals(List.of("1", ""), r.filas().get(1).campos());
    }

    @Test
    void avisaDeComillasSinCerrar() {
        var e = assertThrows(CsvLector.CsvInvalidoException.class, () -> CsvLector.leer("a;b\n1;\"abierta\n2;x"));
        assertEquals(2, e.getLinea());
    }
}
