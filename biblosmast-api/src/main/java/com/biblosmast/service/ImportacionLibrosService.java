package com.biblosmast.service;

import com.biblosmast.model.dto.ImportacionLibrosResponse;
import com.biblosmast.model.dto.ImportarLibrosRequest;
import com.biblosmast.model.dto.LineaImportacionResponse;
import com.biblosmast.model.entity.Autor;
import com.biblosmast.model.entity.Categoria;
import com.biblosmast.model.entity.Editorial;
import com.biblosmast.model.entity.Libro;
import com.biblosmast.repository.AutorRepository;
import com.biblosmast.repository.CategoriaRepository;
import com.biblosmast.repository.EditorialRepository;
import com.biblosmast.repository.LibroRepository;
import com.biblosmast.util.CsvLector;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ImportacionLibrosService implements IImportacionLibrosService {

    static final int MAX_LINEAS = 500;

    /** Nombre de cada columna y los encabezados que se aceptan (ya sin tildes, en minúscula y sin signos). */
    private static final Map<String, List<String>> COLUMNAS = Map.of(
            "isbn", List.of("isbn"),
            "titulo", List.of("titulo"),
            "autores", List.of("autores", "autor"),
            "categoria", List.of("categoria"),
            "editorial", List.of("editorial"),
            "anio", List.of("anio", "ano", "aniopublicacion", "anopublicacion", "aniodepublicacion", "anodepublicacion"),
            "idioma", List.of("idioma"),
            "edicion", List.of("edicion"),
            "paginas", List.of("paginas", "numeropaginas", "numerodepaginas"),
            "descripcion", List.of("descripcion"));
    private static final List<String> OBLIGATORIAS = List.of("isbn", "titulo", "autores", "categoria", "editorial");

    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;
    private final CategoriaRepository categoriaRepository;
    private final EditorialRepository editorialRepository;
    private final IAuditoriaService auditoriaService;

    @Override
    @Transactional
    public ImportacionLibrosResponse importar(ImportarLibrosRequest request, String username) {
        CsvLector.Resultado csv;
        try {
            csv = CsvLector.leer(request.getContenido());
        } catch (CsvLector.CsvInvalidoException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        if (csv.filas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no tiene filas");
        }
        Map<String, Integer> indice = indiceDeColumnas(csv.filas().get(0).campos());
        int columnasEncabezado = csv.filas().get(0).campos().size();
        List<CsvLector.Fila> datos = csv.filas().subList(1, csv.filas().size());
        if (datos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo solo tiene el encabezado: no hay libros");
        }
        if (datos.size() > MAX_LINEAS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo tiene " + datos.size() + " libros: el máximo por importación es " + MAX_LINEAS);
        }

        Catalogos catalogos = cargarCatalogos();
        Map<String, Integer> isbnVistos = new HashMap<>();
        List<LineaImportacionResponse> lineas = new ArrayList<>();
        List<Libro> nuevos = new ArrayList<>();

        for (CsvLector.Fila fila : datos) {
            List<String> errores = new ArrayList<>();
            Libro libro = leerFila(fila, indice, columnasEncabezado, catalogos, isbnVistos, errores);
            lineas.add(LineaImportacionResponse.builder()
                    .numeroLinea(fila.linea())
                    .isbn(valor(fila, indice, "isbn"))
                    .titulo(valor(fila, indice, "titulo"))
                    .errores(errores)
                    .build());
            if (errores.isEmpty()) {
                nuevos.add(libro);
            }
        }

        int conErrores = (int) lineas.stream().filter(l -> !l.getErrores().isEmpty()).count();
        boolean guardar = !request.isSoloValidar() && conErrores == 0;
        if (guardar) {
            libroRepository.saveAll(nuevos);
            auditoriaService.registrarDe(username, "IMPORTAR", "libro", null, nuevos.size() + " libro(s) importados desde CSV");
        }
        return ImportacionLibrosResponse.builder()
                .soloValidar(request.isSoloValidar())
                .importado(guardar)
                .totalLineas(lineas.size())
                .lineasValidas(lineas.size() - conErrores)
                .lineasConErrores(conErrores)
                .librosCreados(guardar ? nuevos.size() : 0)
                .separador(csv.separador() == '\t' ? "tabulador" : String.valueOf(csv.separador()))
                .lineas(lineas)
                .build();
    }

    /** Revisa una fila; si no hay errores devuelve el libro listo para guardar. */
    private Libro leerFila(CsvLector.Fila fila, Map<String, Integer> indice, int columnasEncabezado, Catalogos catalogos,
                           Map<String, Integer> isbnVistos, List<String> errores) {
        if (fila.campos().size() > columnasEncabezado) {
            errores.add("Tiene más columnas que el encabezado: revisa el separador o las comillas");
        }
        String isbn = valor(fila, indice, "isbn");
        String titulo = valor(fila, indice, "titulo");

        if (isbn.isEmpty()) {
            errores.add("Falta el ISBN");
        } else if (isbn.length() > 20) {
            errores.add("El ISBN no puede superar 20 caracteres");
        } else {
            Integer antes = isbnVistos.putIfAbsent(isbn, fila.linea());
            if (antes != null) {
                errores.add("El ISBN " + isbn + " está repetido en el archivo (también en la línea " + antes + ")");
            } else if (libroRepository.existsByIsbn(isbn)) {
                errores.add("Ya existe un libro con el ISBN " + isbn);
            }
        }
        if (titulo.isEmpty()) {
            errores.add("Falta el título");
        } else if (titulo.length() > 200) {
            errores.add("El título no puede superar 200 caracteres");
        }

        Categoria categoria = buscarPorNombre("categoría", valor(fila, indice, "categoria"), catalogos.categorias, errores);
        Editorial editorial = buscarPorNombre("editorial", valor(fila, indice, "editorial"), catalogos.editoriales, errores);
        Set<Autor> autores = buscarAutores(valor(fila, indice, "autores"), catalogos, errores);

        Integer anio = entero("año", valor(fila, indice, "anio"), 1000, 2100, errores);
        Integer paginas = entero("número de páginas", valor(fila, indice, "paginas"), 1, Integer.MAX_VALUE, errores);
        String edicion = valor(fila, indice, "edicion");
        if (edicion.length() > 30) {
            errores.add("La edición no puede superar 30 caracteres");
        }
        String idioma = valor(fila, indice, "idioma");
        if (idioma.length() > 30) {
            errores.add("El idioma no puede superar 30 caracteres");
        }
        if (!errores.isEmpty()) {
            return null;
        }

        Libro libro = new Libro();
        libro.setIsbn(isbn);
        libro.setTitulo(titulo);
        libro.setCategoria(categoria);
        libro.setEditorial(editorial);
        libro.setAnioPublicacion(anio);
        libro.setEdicion(edicion.isEmpty() ? null : edicion);
        libro.setIdioma(idioma.isEmpty() ? "Español" : idioma);
        libro.setNumeroPaginas(paginas);
        String descripcion = valor(fila, indice, "descripcion");
        libro.setDescripcion(descripcion.isEmpty() ? null : descripcion);
        libro.getAutores().addAll(autores);
        return libro;
    }

    private <T> T buscarPorNombre(String que, String nombre, Map<String, T> catalogo, List<String> errores) {
        if (nombre.isEmpty()) {
            errores.add("Falta la " + que);
            return null;
        }
        T encontrado = catalogo.get(clave(nombre));
        if (encontrado == null) {
            errores.add("La " + que + " \"" + nombre + "\" no existe: créala primero en Autores y catálogos");
        }
        return encontrado;
    }

    private Set<Autor> buscarAutores(String texto, Catalogos catalogos, List<String> errores) {
        Set<Autor> resultado = new LinkedHashSet<>();
        if (texto.isEmpty()) {
            errores.add("Falta al menos un autor");
            return resultado;
        }
        boolean hayAutor = false;
        for (String nombre : texto.split("\\|")) {
            String limpio = nombre.trim();
            if (limpio.isEmpty()) {
                continue;
            }
            hayAutor = true;
            List<Autor> candidatos = catalogos.autores.getOrDefault(clave(limpio), List.of());
            if (candidatos.isEmpty()) {
                errores.add("El autor \"" + limpio + "\" no existe: créalo primero en Autores y catálogos");
            } else if (candidatos.size() > 1) {
                errores.add("El autor \"" + limpio + "\" es ambiguo: hay varios con ese nombre");
            } else {
                resultado.add(candidatos.get(0));
            }
        }
        if (!hayAutor) {
            errores.add("Falta al menos un autor");
        }
        return resultado;
    }

    private Integer entero(String que, String texto, int minimo, int maximo, List<String> errores) {
        if (texto.isEmpty()) {
            return null;
        }
        try {
            int n = Integer.parseInt(texto);
            if (n < minimo || n > maximo) {
                errores.add(maximo == Integer.MAX_VALUE
                        ? "El " + que + " debe ser mayor a 0"
                        : "El " + que + " debe estar entre " + minimo + " y " + maximo);
                return null;
            }
            return n;
        } catch (NumberFormatException e) {
            errores.add("El " + que + " \"" + texto + "\" no es un número válido");
            return null;
        }
    }

    // ---------- lectura de columnas y catálogos ----------

    /** Posición de cada columna conocida en el encabezado; falla si falta alguna obligatoria. */
    private Map<String, Integer> indiceDeColumnas(List<String> encabezado) {
        Map<String, Integer> indice = new LinkedHashMap<>();
        for (int i = 0; i < encabezado.size(); i++) {
            String h = clave(encabezado.get(i)).replaceAll("[^a-z0-9]", "");
            for (Map.Entry<String, List<String>> columna : COLUMNAS.entrySet()) {
                if (columna.getValue().contains(h)) {
                    indice.putIfAbsent(columna.getKey(), i);
                }
            }
        }
        List<String> faltan = OBLIGATORIAS.stream().filter(c -> !indice.containsKey(c)).toList();
        if (!faltan.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Faltan columnas obligatorias en el encabezado: " + String.join(", ", faltan)
                            + ". Columnas: isbn, titulo, autores, categoria, editorial (y opcionales: anio, idioma, edicion, paginas, descripcion)");
        }
        return indice;
    }

    private String valor(CsvLector.Fila fila, Map<String, Integer> indice, String columna) {
        Integer posicion = indice.get(columna);
        if (posicion == null || posicion >= fila.campos().size()) {
            return "";
        }
        return fila.campos().get(posicion).trim();
    }

    private Catalogos cargarCatalogos() {
        Map<String, Categoria> categorias = new HashMap<>();
        categoriaRepository.findAll().forEach(c -> categorias.putIfAbsent(clave(c.getNombre()), c));
        Map<String, Editorial> editoriales = new HashMap<>();
        editorialRepository.findAll().forEach(e -> editoriales.putIfAbsent(clave(e.getNombre()), e));
        Map<String, List<Autor>> autores = new HashMap<>();
        for (Autor a : autorRepository.findAll()) {
            String apellidos = a.getApellidos() == null ? "" : a.getApellidos();
            // Se acepta «Nombres Apellidos» y «Apellidos Nombres»
            for (String nombre : Set.of(a.getNombres() + " " + apellidos, apellidos + " " + a.getNombres())) {
                List<Autor> lista = autores.computeIfAbsent(clave(nombre), k -> new ArrayList<>());
                if (!lista.contains(a)) {
                    lista.add(a);
                }
            }
        }
        return new Catalogos(categorias, editoriales, autores);
    }

    /** Minúscula, sin tildes y con espacios simples: «  Gabriel García  Márquez» -> «gabriel garcia marquez». */
    static String clave(String texto) {
        String sinTildes = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private record Catalogos(Map<String, Categoria> categorias, Map<String, Editorial> editoriales,
                             Map<String, List<Autor>> autores) { }
}
