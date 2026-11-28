package com.biblosmast.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Lector de CSV sin dependencias: acepta separador «;» (Excel en español), «,» o tabulador, comillas dobles,
 * comillas escapadas con "" y saltos de línea dentro de un campo entre comillas. Ignora las líneas vacías.
 */
public final class CsvLector {

    /** Una fila con el número de línea del archivo donde empieza (la primera línea es la 1). */
    public record Fila(int linea, List<String> campos) { }

    /** Archivo ya separado en filas, con el separador que se detectó. */
    public record Resultado(char separador, List<Fila> filas) { }

    /** El archivo tiene comillas sin cerrar; {@code linea} es donde empieza el campo problemático. */
    public static class CsvInvalidoException extends RuntimeException {
        private final int linea;

        public CsvInvalidoException(String mensaje, int linea) {
            super(mensaje);
            this.linea = linea;
        }

        public int getLinea() {
            return linea;
        }
    }

    private CsvLector() { }

    public static Resultado leer(String contenido) {
        String texto = contenido.startsWith("\uFEFF") ? contenido.substring(1) : contenido; // marca BOM de Excel
        char separador = detectarSeparador(texto);

        List<Fila> filas = new ArrayList<>();
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        int linea = 1;           // línea física actual
        int lineaInicioFila = 1; // línea donde empezó la fila que se está leyendo
        int lineaComillas = 1;   // línea donde se abrieron las últimas comillas

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (entreComillas) {
                if (c == '"') {
                    if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                        campo.append('"');
                        i++;
                    } else {
                        entreComillas = false;
                    }
                } else {
                    if (c == '\n') {
                        linea++;
                    }
                    campo.append(c);
                }
            } else if (c == '"' && campo.toString().trim().isEmpty()) {
                entreComillas = true;
                lineaComillas = linea;
                campo.setLength(0);
            } else if (c == separador) {
                campos.add(campo.toString());
                campo.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                campos.add(campo.toString());
                campo.setLength(0);
                agregarSiNoEstaVacia(filas, lineaInicioFila, campos);
                campos = new ArrayList<>();
                linea++;
                lineaInicioFila = linea;
            } else {
                campo.append(c);
            }
        }
        if (entreComillas) {
            throw new CsvInvalidoException("Hay comillas sin cerrar que empiezan en la línea " + lineaComillas, lineaComillas);
        }
        campos.add(campo.toString());
        agregarSiNoEstaVacia(filas, lineaInicioFila, campos);
        return new Resultado(separador, filas);
    }

    private static void agregarSiNoEstaVacia(List<Fila> filas, int linea, List<String> campos) {
        if (campos.stream().anyMatch(c -> !c.trim().isEmpty())) {
            filas.add(new Fila(linea, campos));
        }
    }

    /** Mira la primera línea con texto: gana el separador que más aparece (empate o ninguno: «;»). */
    static char detectarSeparador(String texto) {
        for (String primera : texto.split("\\R", -1)) {
            if (primera.isBlank()) {
                continue;
            }
            int puntoComa = cuenta(primera, ';');
            int coma = cuenta(primera, ',');
            int tab = cuenta(primera, '\t');
            if (tab > puntoComa && tab > coma) {
                return '\t';
            }
            return coma > puntoComa ? ',' : ';';
        }
        return ';';
    }

    private static int cuenta(String texto, char c) {
        int n = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }
}
