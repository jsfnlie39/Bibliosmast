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
2. Variables de entorno (no se suben a GitLab):
   - `DB_USER` (por defecto `root`)
   - `DB_PASSWORD` (por defecto vacío)
   - `JWT_SECRET` (opcional): clave para firmar los tokens, mínimo 32 caracteres. Si no se define, la API genera una al azar en cada arranque y los tokens dejan de servir al reiniciarla
   - En STS: `Run > Run Configurations > Environment`.
3. Ejecutar `BiblosmastApplication` (Run As > Spring Boot App). Corre en `http://localhost:8094`.
4. Probar en Swagger: `http://localhost:8094/swagger-ui.html`.

Endpoints (etapas 1, 2 y 3):

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Body `{ "username", "password" }` (`username` acepta el usuario **o el correo**) → datos del usuario, su rol y el **token JWT** (sin password). Es la única ruta pública |
| POST | `/api/auth/registro` | Pública. Crea la persona y su cuenta con rol USUARIO y devuelve la sesión iniciada (máx. 5 por hora por dirección) |
| POST | `/api/auth/recuperar` | Pública. Body `{ "correo" }`. Envía un enlace de recuperación; responde siempre lo mismo, exista o no el correo |
| POST | `/api/auth/restablecer` | Pública. Body `{ "token", "claveNueva" }`. Cambia la clave con el código del enlace (de un solo uso, vale 30 minutos) |
| POST | `/api/auth/logout` | Con sesión. Cierra la sesión: los tokens anteriores dejan de servir |
| GET | `/api/libros?q=texto` | Listar / buscar por título o ISBN |
| GET | `/api/libros/{id}` | Consultar uno |
| POST | `/api/libros` | Crear (201) |
| PUT | `/api/libros/{id}` | Actualizar |
| DELETE | `/api/libros/{id}` | Eliminar (204; 409 si tiene ejemplares) |
| GET | `/api/opciones/categorias`, `/api/opciones/editoriales`, `/api/opciones/autores` | Listas (id y nombre) para los formularios |
| GET, POST | `/api/autores`, `/api/categorias`, `/api/editoriales` | Listar y crear (201) |
| GET, PUT, DELETE | `/api/autores/{id}`, `/api/categorias/{id}`, `/api/editoriales/{id}` | Consultar, actualizar y eliminar (409 si tiene libros asociados) |
| GET | `/api/usuarios/activos`, `/api/ejemplares/disponibles?idUsuario=` | Listas para el formulario de préstamos. Con `idUsuario`, los ejemplares disponibles incluyen los que ese usuario tiene apartados por una reserva lista para recoger |
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
| PUT | `/api/prestamos/{id}/renovar` | Renovar un préstamo (sin body). Devuelve el préstamo con la nueva fecha límite; 409 si no se puede renovar. Se audita como `RENOVACION` |
| GET | `/api/reservas?estado=PENDIENTE\|LISTA\|VENCIDA\|ATENDIDA\|CANCELADA&idUsuario=` | Listar reservas (filtros opcionales). Trae `posicionCola` (turno en la cola), y en las LISTA `fechaLimiteRecogida` y `codigoEjemplar`. Antes de listar se liberan los ejemplares que nadie recogió a tiempo |
| GET | `/api/reservas/{id}` | Consultar una reserva |
| POST | `/api/reservas` | Crear (201). Body: `idUsuario`, `idLibro`, `fechaVencimiento`. Siempre queda PENDIENTE (en cola). 409 si el libro tiene ejemplares disponibles o ninguno prestado |
| PUT | `/api/reservas/{id}` | Actualizar. Body igual más `estado` (`PENDIENTE`, `ATENDIDA` o `CANCELADA`). Una reserva LISTA solo se puede dejar así o cancelar (`CANCELADA`). No se edita una reserva atendida, cancelada o vencida sin recoger |
| DELETE | `/api/reservas/{id}` | Eliminar (204) |

Todas las rutas, salvo el login, exigen el encabezado `Authorization: Bearer <token>` (ver la sección «Seguridad con JWT»).

Errores siempre en JSON: `{ "status": 401, "mensaje": "...", "errores": { "campo": "..." } }`.

## 3. Frontend (VS Code + Node)
El frontend sigue siendo HTML, CSS y JavaScript puro (módulos ES, sin frameworks). **Node.js** se usa como herramienta de desarrollo: Vite sirve las páginas y resuelve la librería de gráficos (Chart.js).

Requisitos: Node.js 20 o superior (`node -v`).

```bash
cd biblosmast-web
npm install        # una sola vez: baja Vite y Chart.js
npm run dev        # abre http://localhost:5173
```

| Comando | Qué hace |
|---|---|
| `npm run dev` | Servidor de desarrollo en `http://localhost:5173` (ya está permitido en CORS) |
| `npm test` | 64 pruebas automáticas (sesión y token, cierre de sesión, clave temporal, registro, recuperación, tablas, panel, menú, tema, multas, renovación, cola de reservas) |
| `npm run build` | Genera la carpeta `dist/` lista para publicar |

Si la API cambia de puerto, editar `js/config.js`.

También se puede abrir con **Live Server**, pero sin Node no hay gráficos: el panel de inicio avisa que hay que usar `npm run dev`; todo lo demás funciona igual.

### Interfaz
- **Menú lateral y barra superior** en todas las páginas, con el menú según el rol; en pantallas pequeñas el menú se oculta y se abre con un botón.
- **Tema claro y oscuro** (se recuerda la elección; al inicio usa la preferencia del sistema).
- **Panel de inicio** con indicadores (libros, ejemplares disponibles, préstamos activos y vencidos, reservas pendientes, usuarios) y **4 gráficos**: ejemplares por estado, préstamos por estado, libros por categoría y préstamos de los últimos 14 días. Cada rol ve solo lo que su rol puede consultar.
- **Tablas:** ordenar al hacer clic en el encabezado (texto, números y fechas), paginación de 10 en 10 y botón **Descargar CSV** (protegido contra fórmulas de Excel).
- **Sesión:** la barra superior muestra cuántos minutos le quedan al token JWT; cuando vence, la página vuelve sola al login.

Archivos nuevos en `biblosmast-web/js/`: `layout.js` (menú, barra y temporizador de sesión), `panel.js` y `estadisticas.js` (indicadores y gráficos), `tablas.js` (ordenar, paginar, CSV), `tema.js` e `iconos.js`.

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
  - [x] Multas por retraso, daño o pérdida
  - [x] Reservas (con cola: la reserva pasa a «Lista para recoger» al devolverse un ejemplar)
  - [x] Auditoría

## Cómo funciona el préstamo
- Solo ADMIN y BIBLIOTECARIO entran al módulo. El préstamo lo registra la persona que inició sesión.
- No se presta a usuarios que no estén ACTIVO, ni ejemplares que no estén DISPONIBLES (409 con el código del ejemplar).
- Un préstamo se registra con 1 a 5 ejemplares y una fecha límite posterior a hoy.
- Los triggers de la base de datos cambian solos el estado del ejemplar: al prestar queda PRESTADO; al devolver queda DISPONIBLE, DANADO o PERDIDO.
- Si el usuario tenía una reserva vigente de alguno de los libros, esa reserva pasa a ATENDIDA al registrar el préstamo.
- El préstamo pasa a FINALIZADO cuando se devuelven todos sus ejemplares. Se muestra como VENCIDO si sigue activo y pasó la fecha límite (con los días de retraso).

## Reservas (con cola)
- Solo ADMIN y BIBLIOTECARIO entran al módulo. Una reserva pone a un usuario en la **cola de un libro**; el orden es por fecha de reserva (`posicionCola`: turno 1 = el siguiente).
- **Solo se reserva si el libro no tiene ningún ejemplar disponible** (409 «tiene ejemplares disponibles: regístrale el préstamo») y si tiene al menos uno prestado que pueda volver (409 si no).
- Solo reservan usuarios ACTIVO y libros ACTIVO. La fecha de vencimiento debe ser posterior a hoy. Un usuario no puede tener dos reservas abiertas (en cola o lista) del mismo libro (409).
- Al crear queda PENDIENTE («En cola»). **Cuando se devuelve un ejemplar en buen estado**, el ejemplar queda RESERVADO y la primera reserva vigente de la cola (de un usuario ACTIVO) pasa a **LISTA**: tiene `dias_recogida_reserva` días (3 de ejemplo) para recoger. La respuesta de la devolución trae `apartadoPara` con el nombre y la pantalla de Préstamos lo avisa.
- Una reserva LISTA se atiende **registrando el préstamo**: ese ejemplar solo se le puede prestar a quien lo tiene apartado (en el formulario aparece como «apartado para este usuario»; para los demás es 409). La reserva pasa sola a ATENDIDA. Si el usuario se lleva otro ejemplar del mismo libro, el apartado se libera para el siguiente de la cola.
- Si la reserva LISTA se **cancela, se elimina, vence sin recogerse o el usuario se da de baja**, el ejemplar apartado pasa al siguiente de la cola (o vuelve a DISPONIBLE si no hay nadie). Una LISTA que venció queda guardada como VENCIDA y ya no se edita.
- No se puede editar ni eliminar un ejemplar que está apartado para una reserva LISTA (409): primero se cancela la reserva. Si un ejemplar queda DISPONIBLE al crearlo o editarlo y hay cola, se aparta para el primero.
- Una PENDIENTE que pasó su fecha de vencimiento se muestra como Vencida (no se guarda) y se puede cancelar o reactivar con una nueva fecha; la cola la salta. Eliminar una reserva la borra del todo.
- Al registrar un préstamo, las reservas PENDIENTE vigentes del usuario para los libros que se lleva también pasan a ATENDIDA (la respuesta del `POST /api/prestamos` trae `reservasAtendidas`, que cuenta las LISTA y las PENDIENTE).
- **Sin tareas automáticas todavía:** los plazos de recogida vencidos se liberan al abrir Reservas, al registrar un préstamo o al crear/editar una reserva. Cuando existan las tareas `@Scheduled` (punto 11), lo harán solas cada día.
- El panel cuenta como «Reservas pendientes» las que están en cola y las listas para recoger.
- **Hay que volver a ejecutar `biblosmast_db.sql`**: la tabla `reserva` tiene el estado `LISTA` y las columnas `fecha_limite_recogida` e `id_ejemplar`, y hay una clave nueva en `configuracion` (`dias_recogida_reserva`). Las reservas de ejemplo no cambian: siguen PENDIENTE aunque el libro tenga ejemplares libres (la cola se mueve cuando algo se devuelve).

**Pruebas de la cola de reservas** (con `admin` o `biblio`; base recién creada con el script)
1. Reservas > Nueva reserva de `laura` para *Cosmos* (tiene su ejemplar libre): 409 «tiene ejemplares disponibles».
2. Préstamos: prestar `LIB-0002-A` (*Clean Code*) a `laura`. Reservas > Nueva reserva de `biblio` (Beatriz) para *Clean Code*: ahora sí se puede. La tabla muestra a `carlos` con «Turno 1» (su reserva de ejemplo) y a Beatriz con «Turno 2». Si pasaron más de 3 días desde que corriste el script, la de `carlos` aparece Vencida: reactívala con una fecha nueva.
3. Devolver `LIB-0002-A` en «Buen estado»: el mensaje dice «El ejemplar quedó apartado para Carlos Mejia, que ya puede recogerlo». En Reservas, `carlos` queda «Lista para recoger» con «Recoger hasta (hoy + 3 días) · LIB-0002-A» y Beatriz pasa a «Turno 1». En Ejemplares, `LIB-0002-A` está RESERVADO.
4. Nuevo préstamo: con otro usuario, `LIB-0002-A` no aparece en la lista; al elegir `carlos` aparece «(apartado para este usuario)». Probar por Swagger un préstamo de `LIB-0002-A` a `laura`: 409 «Ejemplares no disponibles».
5. Ejemplares: editar o eliminar `LIB-0002-A`: 409 «apartado para una reserva lista para recoger».
6. Prestar `LIB-0002-A` a `carlos`: «Se atendió 1 reserva del usuario»; su reserva queda Atendida y Beatriz sigue en «Turno 1». Devolverlo en buen estado: queda apartado para Beatriz.
7. Editar la reserva de Beatriz y ponerla en «Cancelada»: `LIB-0002-A` vuelve a DISPONIBLE (ya no hay nadie en la cola). Repetir 2 y 3 y, con una reserva LISTA, ejecutar en DBeaver `UPDATE reserva SET fecha_limite_recogida = DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY) WHERE estado = 'LISTA';` y abrir Reservas: queda Vencida y el ejemplar pasa al siguiente o a DISPONIBLE.
8. Dar de baja (Usuarios) a alguien con una reserva LISTA, sin préstamos ni multas pendientes: su reserva queda Cancelada y el ejemplar pasa al siguiente de la cola. En Auditoría aparece `CANCELAR`.

## Multas

Se generan solas al **devolver un ejemplar** (por ejemplar):
- **Perdido:** multa fija de pérdida (sin retraso).
- **Dañado:** multa fija de daño, más el retraso si se devolvió tarde.
- **Buen estado:** solo el retraso (días de retraso × valor por día).

Los tres valores están en la tabla `configuracion` (`multa_por_dia_retraso` 1.000, `multa_dano` 10.000, `multa_perdida` 30.000 pesos de ejemplo) y los cambia el **ADMIN** desde la pantalla de Multas. Un valor en 0 desactiva ese cobro. Un usuario con multas pendientes **no puede recibir préstamos** (409).

| Ruta | Para qué | Quién |
|---|---|---|
| `GET /api/multas?estado=&idUsuario=` | Listar multas | ADMIN y BIBLIOTECARIO |
| `PUT /api/multas/{id}/pago` | Registrar el pago (se audita como `PAGO`) | ADMIN y BIBLIOTECARIO |
| `DELETE /api/multas/{id}` | Anular una multa pendiente | Solo ADMIN |
| `GET/PUT /api/configuracion` | Ver y cambiar los valores | Solo ADMIN |

La respuesta de la devolución trae `multasGeneradas` y `valorMultasGeneradas`, y la pantalla de Préstamos avisa cuando hubo multas.

**Pruebas de multas**
1. Como `admin`, abrir Multas y cambiar «multa por día de retraso» a 2000: «Valor actualizado».
2. En HeidiSQL, poner un préstamo activo con la fecha vencida: `UPDATE prestamo SET fecha_limite = DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY) WHERE id_prestamo = <N> AND estado = 'ACTIVO';`
3. Devolver un ejemplar de ese préstamo en buen estado: aviso «Se generó 1 multa por $6.000» y aparece en Multas como pendiente.
4. Intentar un préstamo nuevo para ese usuario: 409 «tiene multas pendientes».
5. Registrar el pago como `biblio`: pasa a «Pagada» y ya se puede prestar. Como `biblio` no se ve el botón «Anular» (solo `admin`).
6. Con ese usuario aún con multa pendiente, intentar darlo de baja como `admin`: 409 «tiene multas pendientes de pago». Tras registrar el pago, la baja sí funciona.
7. Devolver otro ejemplar como Dañado y otro como Perdido: multas de 10.000 y 30.000. En Auditoría aparece `PAGO`.

## Renovar préstamos

`PUT /api/prestamos/{id}/renovar` (ADMIN y BIBLIOTECARIO). El botón «Renovar» aparece en Préstamos solo en los activos. Reglas (todas responden 409 con el motivo):
- El préstamo no puede estar finalizado ni vencido.
- Máximo de renovaciones: `max_renovaciones` (2 de ejemplo). La columna `prestamo.renovaciones` cuenta las usadas.
- El usuario no puede tener multas pendientes.
- Ningún libro que siga prestado puede tener una reserva pendiente y vigente de **otra** persona.
- La nueva fecha límite es la mayor entre la actual y hoy, más `dias_prestamo` (7 de ejemplo).

Los dos valores están en la tabla `configuracion`. No aparecen en la pantalla de Multas (solo muestra las claves `multa_*`); el ADMIN los cambia con `PUT /api/configuracion/{clave}` (por ejemplo desde Swagger). **Hay que volver a ejecutar `biblosmast_db.sql`** porque cambia la tabla `prestamo` y se agregan dos claves.

**Pruebas de renovación** (con `admin` o `biblio`)
1. Crear un préstamo para `carlos` y pulsar «Renovar»: «Nueva fecha límite» sube 7 días y la columna Renovaciones dice «1 vez». En Auditoría aparece `RENOVACION`.
2. Renovar otra vez (2 veces) y una tercera: 409 «ya se usaron las 2 renovaciones permitidas».
3. Prestar `LIB-0002-A` (*Clean Code*, un solo ejemplar) a `carlos`; crear una reserva vigente de `laura` para *Clean Code* (ahora se puede porque no queda ejemplar libre) y renovar el préstamo de `carlos`: 409 «otra persona tiene reservado alguno de estos libros». Cancelar la reserva y renovar: funciona.
4. Dar a un usuario una multa pendiente (devolver un ejemplar tarde en otro préstamo) e intentar renovar su préstamo activo: 409 «multas pendientes».
5. Intentar renovar un préstamo finalizado: 409. Un préstamo vencido: 409 (los de prueba traen la fecha límite en el futuro; para probarlo hay que cambiar `fecha_limite` en la base).

## Usuarios y roles
- Solo ADMIN entra al módulo. Crear un usuario crea a la vez su cuenta (usuario, contraseña cifrada y rol).
- Al editar, si la contraseña va vacía no se cambia. Documento, correo y nombre de usuario no se pueden repetir (409).
- No se puede dar de baja, desactivar ni cambiar de rol al último administrador activo.
- **Eliminar un usuario es una baja lógica** (ver «Fase 1b»): el usuario no se borra, su cuenta se bloquea y se conserva todo su historial. No se puede dar de baja a quien tiene préstamos sin devolver ni multas pendientes; al darlo de baja se cancelan sus reservas pendientes y las listas para recoger (el ejemplar apartado pasa al siguiente de la cola).

## Seguridad con JWT
**Flujo**
1. El frontend envía usuario y clave a `POST /api/auth/login` (JSON con `fetch`).
2. El backend busca la cuenta, compara la clave con BCrypt y, si es correcta, crea un **token JWT** firmado (HS256) con el usuario (`sub`), su id (`uid`), su rol (`rol`), la fecha de emisión (`iat`) y la de vencimiento (`exp`, 60 minutos por defecto).
3. El frontend guarda el token en `sessionStorage` y lo envía en **cada petición**: `Authorization: Bearer <token>`.
4. En el backend, `JwtAuthFilter` revisa la firma y el vencimiento de cada token; si sirve, deja al usuario autenticado con su rol. Las reglas de `SeguridadWebConfig` deciden si ese rol puede entrar a la ruta.
5. Respuestas: **401** si falta el token o está vencido o alterado; **403** si el rol no tiene permiso. El frontend, ante un 401, borra la sesión y vuelve al login.

**Quién puede entrar a qué**
| Ruta | Quién |
|---|---|
| `POST /api/auth/login`, Swagger | Todos (sin token) |
| `GET /api/libros/**` | Cualquier usuario con sesión |
| `GET/PUT /api/perfil`, `PUT /api/perfil/clave`, `POST /api/auth/logout` | Cualquier usuario con sesión (la persona sale del token) |
| `/api/usuarios/**`, `/api/roles`, `/api/auditoria`, `/api/configuracion/**` | Solo ADMIN (excepto `GET /api/usuarios/activos`, también BIBLIOTECARIO) |
| **Eliminar** libros, ejemplares, reservas, autores, categorías, editoriales y **anular multas** | Solo ADMIN |
| Crear y editar libros, ejemplares, reservas, catálogos; préstamos y devoluciones; ver multas y registrar su pago | ADMIN y BIBLIOTECARIO |

**Archivos del backend** (`biblosmast-api/src/main/java/com/biblosmast/security/`)
| Archivo | Para qué sirve |
|---|---|
| `JwtService` | Crea y valida los tokens |
| `JwtAuthFilter` | Lee el token de cada petición y autentica al usuario |
| `SeguridadWebConfig` | Reglas por ruta y rol, CORS, API sin sesión (stateless) |
| `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` | Respuestas 401 y 403 en JSON |
| `OpenApiConfig` | Botón *Authorize* de Swagger |
| `TokenGenerado` | Token + momento de vencimiento |

**Archivos del frontend** (`biblosmast-web/js/`): `sesion.js` (guarda el token y lo lee), `api.js` (agrega el encabezado y maneja el 401), `auth.js` (login, logout y revisión de vencimiento), `login.js` y `menu.js` (muestra el contenido del token).

**Cómo probarlo**
1. Con la API corriendo, en Swagger (`/swagger-ui.html`): ejecutar `POST /api/auth/login` con `admin` / `Admin123*`, copiar el valor de `token`, pulsar **Authorize** y pegarlo. Ahora las demás rutas responden.
2. Sin token: `curl.exe -i http://localhost:8094/api/libros` → **401**.
3. Con token de un usuario con rol USUARIO (`carlos`) llamar a `GET /api/usuarios` → **403**.
4. Con un token alterado (cambiar un carácter) o vencido → **401**.
5. En el navegador: iniciar sesión, abrir el menú y ver el panel «Tu token JWT».

## Fase 1: acceso y seguridad
Sobre el login con JWT se agregó:

| Función | Cómo funciona |
|---|---|
| **Mi perfil** | `GET/PUT /api/perfil` y `PUT /api/perfil/clave`. Cada persona edita su correo y teléfono y cambia su contraseña (pide la actual). La persona siempre sale del token, así que nadie puede tocar el perfil de otro |
| **Bloqueo por intentos fallidos** | 5 claves incorrectas seguidas bloquean la cuenta 15 minutos (`seguridad.max-intentos` y `seguridad.bloqueo-minutos` en `application.properties`). Responde 423 y queda en auditoría |
| **Cerrar sesión de verdad** | `POST /api/auth/logout` anota en `cuenta.token_desde` el momento del cierre: los tokens emitidos antes ya no valen. Lo mismo pasa al cambiar o restablecer la clave. Desactivar una cuenta también invalida sus tokens |
| **Rol siempre al día** | En cada petición el filtro consulta la cuenta en la base de datos: usa el rol actual (no el escrito en el token) y rechaza cuentas inactivas |
| **Restablecer contraseña (ADMIN)** | `PUT /api/usuarios/{id}/clave-temporal` genera una clave temporal de 10 caracteres, la muestra una sola vez al administrador, desbloquea la cuenta, cierra sus sesiones y obliga a cambiarla: mientras tanto el usuario solo puede entrar a «Mi perfil» (el resto responde 403) |
| **Permisos más finos** | Eliminar registros es solo de ADMIN; el bibliotecario crea y edita pero no borra. El frontend oculta esos botones |
| **Auditoría** | Solo ADMIN. Registra inicios de sesión (buenos, fallidos y bloqueos), cierres de sesión, cambios y restablecimientos de clave, y cada cambio de datos. La persona sale del token validado, no del navegador |

Cambios en la base de datos (fase 1): la tabla `cuenta` tiene 4 columnas nuevas (`intentos_fallidos`, `bloqueado_hasta`, `debe_cambiar_clave`, `token_desde`) y `auditoria` un índice por fecha. Hay que volver a ejecutar `biblosmast_db.sql` (borra y recrea la base).

**Pruebas de la fase 1**
1. Escribir 5 veces una clave incorrecta con `laura`: la 5.ª responde 423; con la clave correcta también 423 hasta que pasen 15 minutos (o el administrador restablezca la clave).
2. Iniciar sesión, copiar el token, cerrar sesión y usar ese token en Swagger: 401 «Tu sesión fue cerrada».
3. Como `admin`, restablecer la clave de `carlos`; entrar como `carlos` con la clave temporal: lleva a «Mi perfil»; cualquier otra ruta responde 403 hasta cambiarla.
4. Cambiar la clave en «Mi perfil»: la sesión continúa con un token nuevo y las demás sesiones se cierran.
5. Como `biblio`, intentar eliminar un libro: no aparece el botón; por la API (Swagger) responde 403.
6. Como `admin`, abrir Auditoría y ver los registros de los pasos anteriores.

## Fase 1b: cuentas (usuario o correo, baja lógica, registro y recuperación)
| Función | Cómo funciona |
|---|---|
| **Entrar con usuario o correo** | El campo `username` del login acepta cualquiera de los dos (sin distinguir mayúsculas). Solo cuentas vigentes |
| **Baja lógica de usuarios** | `DELETE /api/usuarios/{id}` ya no borra: marca `usuario.eliminado = 1` y `cuenta.eliminada = 1`, bloquea la cuenta, cierra sus sesiones y la deja inactiva. **Se conserva todo el historial** (préstamos, reservas, multas y auditoría siguen apuntando a esa persona). No se permite si tiene préstamos sin devolver ni si es el último administrador |
| **Volver a registrarse** | Documento, correo y nombre de usuario solo son únicos entre usuarios vigentes (columnas calculadas `documento_vigente`, `email_vigente`, `username_vigente`, que valen NULL al dar de baja). Quien fue dado de baja puede registrarse de nuevo con los mismos datos y obtiene una cuenta nueva; la anterior queda guardada |
| **Ver dados de baja** | `GET /api/usuarios?eliminados=true` (solo ADMIN). En la pantalla Usuarios: casilla «Ver usuarios dados de baja» (solo consulta) |
| **Crear cuenta** | Pantalla `registro.html` → `POST /api/auth/registro`. Rol USUARIO, estado ACTIVO, sesión iniciada al terminar |
| **Recuperar contraseña** | `recuperar.html` pide el correo → la API crea un código aleatorio de 32 bytes (en la base solo se guarda su hash SHA-256), vence a los 30 minutos y es de un solo uso; un enlace nuevo invalida los anteriores; máximo uno por minuto por cuenta. `restablecer.html?token=…` pide la clave nueva, que además cierra las sesiones abiertas |
| **Correo** | Se envía con `spring-boot-starter-mail` solo si se definen las variables de entorno `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME` y `SPRING_MAIL_PASSWORD` (y opcionalmente `MAIL_FROM`). **Sin ellas, el enlace aparece en la consola de la API** (modo desarrollo). Para Gmail: `SPRING_MAIL_HOST=smtp.gmail.com`, `SPRING_MAIL_PORT=587`, el usuario es la cuenta de Gmail, la contraseña es una **contraseña de aplicación** de 16 letras (requiere verificación en dos pasos) y `MAIL_FROM` debe ser esa misma cuenta; `auth` y `starttls` ya están en `application.properties` |

Cambios en la base de datos (fase 1b): `usuario` (+`eliminado`, `fecha_baja` y 2 columnas calculadas con su UNIQUE), `cuenta` (+`eliminada` y 1 columna calculada con su UNIQUE), y la tabla nueva `recuperacion_clave`. La base queda con **15 tablas**. Hay que volver a ejecutar `biblosmast_db.sql`.

**Pruebas de la fase 1b**
1. Entrar con el correo de `laura` (`laura@biblosmast.com`) y con su usuario: ambos funcionan.
2. Como `admin`, crear un usuario `prueba1` con el correo `prueba1@x.com` y darlo de baja: ya no aparece en la lista y no puede entrar; con «Ver usuarios dados de baja» sí aparece. Sus movimientos siguen en Auditoría.
3. Registrarse de nuevo en `registro.html` con el mismo correo, documento y usuario: se crea una cuenta nueva y la anterior sigue guardada.
4. «¿Olvidaste tu contraseña?» con ese correo: copiar el enlace desde la consola de la API, abrirlo y elegir una clave nueva; entrar con ella. Usar el mismo enlace otra vez: «El enlace no es válido o ya venció».
5. Intentar dar de baja a un usuario con un préstamo sin devolver: 409. Intentar dar de baja al único administrador: 409.
6. Dar de baja a `laura` (o a quien tenga una reserva pendiente o lista) sin préstamos ni multas pendientes: sus reservas pasan a «Cancelada» en Reservas y en Auditoría aparece la acción `CANCELAR`.

## Limitaciones conocidas
- El token se guarda en `sessionStorage`: se borra al cerrar la pestaña, pero un ataque XSS podría leerlo. El frontend crea los elementos con `textContent` para evitarlo. En producción se recomienda HTTPS y valorar una cookie `HttpOnly`.
- No hay *refresh token*: cuando el token vence (60 minutos) hay que iniciar sesión de nuevo.
- Revocar un token requiere una consulta a la base de datos en cada petición (es el costo de poder cerrar sesión de verdad).
- Si una sesión se cierra y se vuelve a iniciar dentro del mismo segundo, ambos tokens quedan válidos (la comparación se hace en segundos).
- El registro público no verifica el correo (no se envía un código de confirmación) y el límite de solicitudes vive en la memoria de la API (se borra al reiniciarla).
- El borrado de libros es físico; si tiene ejemplares, la API responde 409.
