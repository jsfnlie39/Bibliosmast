# Biblosmast

Sistema de gestión de biblioteca, versión desacoplada.

- `db/` → script MySQL (`biblosmast_db.sql`)
- `biblosmast-api/` → API REST (Java 17, Spring Boot 2.7.3, Maven) · solo JSON
- `biblosmast-web/` → Frontend (HTML5 + CSS3 + JavaScript puro, módulos ES)

## 1. Base de datos (DBeaver)
1. Conectar DBeaver a MySQL 8.
2. Abrir `db/biblosmast_db.sql` y ejecutarlo completo (Alt+X). Crea la base `biblosmast` con datos de prueba.
3. Cuentas de prueba (todas con clave **Admin123\***): `admin` (administrador), `biblio` (bibliotecario), `carlos` y `laura` (usuarios) (cambiar la clave luego).

## 2. Backend (Spring Tool Suite)
1. `File > Import > Existing Maven Projects` → carpeta `biblosmast-api`.
2. Credenciales de MySQL por variables de entorno (no se suben a GitLab):
   - `DB_USER` (por defecto `root`)
   - `DB_PASSWORD` (por defecto vacío)
   - En STS: `Run > Run Configurations > Environment`.
3. Ejecutar `BiblosmastApplication` (Run As > Spring Boot App). Corre en `http://localhost:8094`.
4. Probar en Swagger: `http://localhost:8094/swagger-ui.html`.

Endpoints (etapas 1, 2 y 3):

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Body `{ "username", "password" }` → datos del usuario y rol (sin password) |
| GET | `/api/libros?q=texto` | Listar / buscar por título o ISBN |
| GET | `/api/libros/{id}` | Consultar uno |
| POST | `/api/libros` | Crear (201) |
| PUT | `/api/libros/{id}` | Actualizar |
| DELETE | `/api/libros/{id}` | Eliminar (204; 409 si tiene ejemplares) |
| GET | `/api/opciones/categorias`, `/api/opciones/editoriales`, `/api/opciones/autores` | Listas (id y nombre) para los formularios |
| GET, POST | `/api/autores`, `/api/categorias`, `/api/editoriales` | Listar y crear (201) |
| GET, PUT, DELETE | `/api/autores/{id}`, `/api/categorias/{id}`, `/api/editoriales/{id}` | Consultar, actualizar y eliminar (409 si tiene libros asociados) |
| GET | `/api/usuarios/activos`, `/api/ejemplares/disponibles` | Listas para el formulario de préstamos |
| GET | `/api/ejemplares?idLibro=` | Listar ejemplares (todos o de un libro) |
| GET | `/api/ejemplares/{id}` | Consultar un ejemplar |
| POST | `/api/ejemplares` | Registrar ejemplar (201). Queda DISPONIBLE por defecto |
| PUT | `/api/ejemplares/{id}` | Actualizar (no se edita uno que esté prestado) |
| DELETE | `/api/ejemplares/{id}` | Eliminar (204; 409 si está prestado o tiene historial de préstamos) |
| GET | `/api/roles` | Lista de roles para el formulario de usuarios |
| GET | `/api/usuarios?q=texto` | Listar / buscar usuarios (nombre, documento, correo o usuario) |
| GET | `/api/usuarios/{id}` | Consultar un usuario con su cuenta |
| POST | `/api/usuarios` | Crear usuario + cuenta (201). La contraseña es obligatoria |
| PUT | `/api/usuarios/{id}` | Actualizar. Si `password` va vacío, no se cambia |
| DELETE | `/api/usuarios/{id}` | Eliminar (204; 409 si tiene préstamos, reservas o multas) |
| GET | `/api/prestamos?estado=ACTIVO\|VENCIDO\|FINALIZADO&idUsuario=` | Listar préstamos; con `idUsuario` es el historial de ese usuario |
| GET | `/api/prestamos/{id}` | Consultar un préstamo con sus ejemplares |
| POST | `/api/prestamos` | Registrar préstamo (201). Body: `idUsuario`, `idBibliotecario`, `fechaLimite`, `idsEjemplares` (máx. 5), `observaciones` |
| PUT | `/api/prestamos/{id}/detalles/{idDetalle}/devolucion` | Devolver un ejemplar. Body `{ "estadoDevolucion": "BUEN_ESTADO\|DANADO\|PERDIDO" }` |
| GET | `/api/reservas?estado=PENDIENTE\|VENCIDA\|ATENDIDA\|CANCELADA&idUsuario=` | Listar reservas (filtros opcionales). Incluye cuántos ejemplares del libro están disponibles |
| GET | `/api/reservas/{id}` | Consultar una reserva |
| POST | `/api/reservas` | Crear (201). Body: `idUsuario`, `idLibro`, `fechaVencimiento`. Siempre queda PENDIENTE |
| PUT | `/api/reservas/{id}` | Actualizar. Body igual más `estado` (`PENDIENTE`, `ATENDIDA` o `CANCELADA`). No se edita una reserva atendida o cancelada |
| DELETE | `/api/reservas/{id}` | Eliminar (204) |

Errores siempre en JSON: `{ "status": 401, "mensaje": "...", "errores": { "campo": "..." } }`.

## 3. Frontend (VS Code)
1. Instalar la extensión **Live Server**.
2. Abrir la carpeta `biblosmast-web`, clic derecho en `index.html` → *Open with Live Server* (puerto 5500).
3. Si usas otro puerto/origen, agrégalo en `app.cors.origins` (`application.properties`).
4. Si la API cambia de puerto, editar `js/config.js`.

## 4. GitLab
```bash
git init
git branch -M main
git remote add origin <URL-del-repositorio-GitLab>
git add .
git commit -m "Etapa 1: BD, login, menu y modulo de libros"
git push -u origin main
```
Flujo sugerido para dos personas: una rama por módulo (`feature/libros`, `feature/usuarios`), commits pequeños y *merge request* hacia `main`.

## Estado por etapas
- [x] Etapa 1: BD, login, menú principal, CRUD de libros y de ejemplares (con disponibilidad en el catálogo)
- [x] Etapa 2: préstamos y devoluciones
- [x] Etapa 3: usuarios y roles (solo ADMIN)
- [ ] Etapa 4, en este orden de prioridad:
  - [x] Autores, categorías y editoriales (CRUD)
  - [ ] Multas por retraso, daño o pérdida
  - [x] Reservas
  - [ ] Auditoría

## Cómo funciona el préstamo
- Solo ADMIN y BIBLIOTECARIO entran al módulo. El préstamo lo registra la persona que inició sesión.
- No se presta a usuarios que no estén ACTIVO, ni ejemplares que no estén DISPONIBLES (409 con el código del ejemplar).
- Un préstamo se registra con 1 a 5 ejemplares y una fecha límite posterior a hoy.
- Los triggers de la base de datos cambian solos el estado del ejemplar: al prestar queda PRESTADO; al devolver queda DISPONIBLE, DANADO o PERDIDO.
- Si el usuario tenía una reserva vigente de alguno de los libros, esa reserva pasa a ATENDIDA al registrar el préstamo.
- El préstamo pasa a FINALIZADO cuando se devuelven todos sus ejemplares. Se muestra como VENCIDO si sigue activo y pasó la fecha límite (con los días de retraso).

## Reservas
- Solo ADMIN y BIBLIOTECARIO entran al módulo. Una reserva aparta un libro (no un ejemplar concreto) para un usuario hasta una fecha de vencimiento.
- Solo reservan usuarios ACTIVO y libros ACTIVO. La fecha de vencimiento debe ser posterior a hoy. Un usuario no puede tener dos reservas pendientes vigentes del mismo libro (409).
- Al crear queda PENDIENTE. Se puede cambiar a ATENDIDA o CANCELADA; después ya no se edita. VENCIDA no se guarda: se muestra cuando una pendiente pasó su fecha de vencimiento.
- Una reserva vencida se puede cancelar o reactivar con una nueva fecha. Eliminar la borra del todo.
- Al registrar un préstamo, las reservas pendientes y vigentes de ese usuario para los libros que se lleva pasan solas a ATENDIDA (la respuesta del `POST /api/prestamos` trae `reservasAtendidas`). Una reserva vencida no se atiende sola. También se puede marcar ATENDIDA a mano.
- Un préstamo no revisa si otra persona tiene reservado el último ejemplar: la reserva avisa, pero no bloquea.

## Usuarios y roles
- Solo ADMIN entra al módulo. Crear un usuario crea a la vez su cuenta (usuario, contraseña cifrada y rol).
- Al editar, si la contraseña va vacía no se cambia. Documento, correo y nombre de usuario no se pueden repetir (409).
- No se puede eliminar, desactivar ni cambiar de rol al último administrador activo.
- Si el usuario tiene préstamos, reservas o multas, no se elimina: se cambia a estado INACTIVO.

## Limitaciones conocidas
- El login valida usuario y contraseña en el backend. El control de sesión se hace en el frontend (`sessionStorage`) y el rol solo oculta botones; la API no vuelve a pedir credenciales en cada petición.
- El borrado de libros es físico; si tiene ejemplares, la API responde 409.
