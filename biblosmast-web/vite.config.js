import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

// Aplicación de varias páginas HTML: cada una es un punto de entrada del build.
const paginas = ["index", "menu", "libros", "ejemplares", "prestamos", "reservas", "multas", "catalogos", "usuarios", "perfil", "auditoria", "registro", "recuperar", "restablecer"];

export default defineConfig({
  // 5173 ya está permitido en CORS (app.cors.origins del backend)
  server: { port: 5173, strictPort: true },
  build: {
    rollupOptions: {
      input: Object.fromEntries(
        paginas.map((p) => [p, fileURLToPath(new URL(`./${p}.html`, import.meta.url))]),
      ),
    },
  },
});
