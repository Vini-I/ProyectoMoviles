# Pura Guía — Base del Proyecto (Android Studio + Kotlin + XML + SQLite)

Proyecto base para el **MVP 1** del curso, basado en los wireframes de **Pura Guía** (*Descubre Costa Rica*).
Incluye la navegación completa entre pantallas, el diseño XML de toda la aplicación y la lógica funcional con **SQLite** para **Inicio de Sesión, Registro (Turista y Guía), Sesión Persistente y Gestión de Cuenta**.

El resto de módulos (Tours, Búsqueda, Reservas, Reseñas, Favoritos, Métodos de Pago, Mensajería) ya tienen sus pantallas y navegación listas con datos de ejemplo (`SampleData`), marcadas con comentarios `TODO(equipo ...)` para que cada integrante agregue su lógica y sus tablas en SQLite.

---

## Cómo abrir y compilar en Android Studio

1. Descomprime `PuraGuia.zip`.
2. En **Android Studio**, elige **Open** y selecciona la carpeta `PuraGuia`.
3. Espera a que Gradle sincronice dependencias y presiona **Run ▶**.
4. Si en tu PC ya tienes Gradle / SDK configurado con otra versión, puedes usar **File → Sync Project with Gradle Files**.

---

## Estructura del proyecto (`com.puraguia.app`)

```text
app/src/main/java/com/puraguia/app/
├── data/
│   ├── DatabaseHelper.kt     # SQLiteOpenHelper único ("pura_guia.db")
│   ├── Usuario.kt            # Modelo de usuario (TURISTA / GUIA)
│   ├── UsuarioDao.kt         # CRUD completo de la tabla `usuarios`
│   └── SampleData.kt         # Datos de ejemplo mientras se conectan los demás DAOs
├── model/
│   └── Models.kt             # Modelos base (Tour, Chat, Mensaje, Resena, Tarjeta, Actividad...)
├── util/
│   ├── SessionManager.kt     # Sesión persistente en SharedPreferences
│   ├── AuthGuard.kt          # Regla de navegación pública vs. acciones que piden login
│   ├── PasswordHasher.kt     # Hash SHA-256 + salt por usuario
│   ├── Validators.kt         # Validación de email, teléfono, nombre y contraseña
│   ├── FileUtils.kt          # Copia segura de fotos/certificados al almacenamiento interno
│   └── UiExt.kt              # Helpers UI (setupHeader, ChipGrid, iniciales, toast, dp)
└── ui/
    ├── splash/SplashActivity.kt
    ├── main/
    │   ├── MainActivity.kt         # BottomNavigationView (Inicio, Buscar, Mensajes, Perfil)
    │   ├── HomeFragment.kt
    │   ├── SearchFragment.kt
    │   ├── MessagesFragment.kt
    │   └── ProfileFragment.kt
    ├── auth/
    │   ├── LoginActivity.kt
    │   ├── RegisterRoleActivity.kt
    │   ├── RegisterTouristActivity.kt
    │   └── RegisterGuideActivity.kt
    ├── tour/
    │   ├── TourDetailActivity.kt
    │   ├── ReservaActivity.kt
    │   └── GuideProfileActivity.kt
    ├── chat/
    │   └── ChatActivity.kt
    ├── profile/
    │   ├── FavoritosActivity.kt
    │   ├── MetodosPagoActivity.kt
    │   ├── ConfiguracionActivity.kt
    │   └── ActividadRecienteActivity.kt
    └── common/
        └── TourCardFactory.kt
```

---

## Regla de navegación (Invitado vs. Sesión iniciada)

La aplicación permite navegar libremente por **Inicio**, **Buscar**, **Detalle de Tour** y **Perfil público del guía** sin haber iniciado sesión.

Cuando el usuario intenta realizar una acción protegida, se bloquea y redirige al login usando `AuthGuard.requireLogin`:

- Entrar a la pestaña **Mensajes** o **Perfil** (`MainActivity.kt`)
- **Reservar un tour** (`TourDetailActivity.kt`)
- **Enviar un mensaje** (`TourDetailActivity.kt`, `GuideProfileActivity.kt`, `ChatActivity.kt`)
- **Guardar en favoritos** o **escribir una reseña** (`TourDetailActivity.kt`)

### Cómo proteger nuevas acciones en sus pantallas

```kotlin
AuthGuard.requireLogin(this, "Inicia sesión para continuar") {
    // Este bloque SOLO se ejecuta si hay una sesión activa
}
```

### Cómo obtener el usuario conectado

```kotlin
val session = SessionManager(this)
if (session.isLoggedIn()) {
    val userId = session.getUserId()   // FK hacia usuarios(id) en SQLite
    val nombre = session.getNombre()
    val rol = session.getRol()         // "TURISTA" o "GUIA"
}
```

---

## Base de datos SQLite (`pura_guia.db`)

Ya está implementada la tabla **`usuarios`** en [`DatabaseHelper.kt`](app/src/main/java/com/puraguia/app/data/DatabaseHelper.kt) y su DAO en [`UsuarioDao.kt`](app/src/main/java/com/puraguia/app/data/UsuarioDao.kt):

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | Identificador único |
| `rol` | `TEXT NOT NULL` | `'TURISTA'` o `'GUIA'` |
| `nombre` | `TEXT NOT NULL` | Nombre completo |
| `email` | `TEXT NOT NULL UNIQUE` | Correo único normalizado |
| `telefono` | `TEXT NOT NULL` | Teléfono |
| `pais` | `TEXT` | País (para turista) |
| `intereses` | `TEXT` | Lista separada por `\|` (para turista) |
| `idiomas` | `TEXT` | Lista separada por `\|` (para guía) |
| `ubicaciones` | `TEXT` | Provincias separadas por `\|` (para guía) |
| `certificaciones` | `TEXT` | Rutas internas separadas por `\|` (para guía) |
| `foto_path` | `TEXT` | Ruta interna de la foto (para guía) |
| `password_hash` | `TEXT NOT NULL` | Hash SHA-256 con salt |
| `salt` | `TEXT NOT NULL` | Salt aleatorio por usuario |
| `fecha_registro` | `INTEGER NOT NULL` | Timestamp de creación |

### Cómo agregar el resto de tablas para el equipo

1. En `DatabaseHelper.kt`, definan las constantes y la sentencia `CREATE TABLE` de su módulo (por ejemplo `tours`, `reservas`, `favoritos`, `resenas`, `mensajes`, `metodos_pago`, `actividad_reciente`), con `FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE` cuando corresponda.
2. Ejecuten `db.execSQL(...)` dentro de `onCreate(db)`.
3. Incrementen `DB_VERSION` y agreguen el `if (oldVersion < N)` en `onUpgrade` (o desinstalen la app del emulador para recrear la base).
4. Creen un DAO propio por tabla (por ejemplo `TourDao`, `ReservaDao`) siguiendo el estilo de `UsuarioDao.kt` y reemplacen gradualmente las listas de `SampleData.kt`.
5. Busquen `TODO(equipo` en Android Studio (**Ctrl + Shift + F**) para ver exactamente dónde conectar cada pantalla.
