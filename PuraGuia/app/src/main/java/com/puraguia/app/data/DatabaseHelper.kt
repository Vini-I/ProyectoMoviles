package com.puraguia.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Base de datos SQLite de Pura Guía.
 *
 * Es UNA sola base de datos para toda la app. Cada integrante agrega SUS tablas aquí:
 *
 *  1) Declarar el nombre de la tabla y su sentencia CREATE TABLE (como CREATE_USUARIOS).
 *  2) Ejecutarla dentro de [onCreate] (para instalaciones nuevas).
 *  3) Subir DB_VERSION y ejecutarla dentro de [onUpgrade] con `if (oldVersion < N)`
 *     (para quienes ya tienen la app instalada). Mientras desarrollan también
 *     pueden desinstalar la app del emulador para que se recree la BD.
 *  4) Crear su propio DAO (ver UsuarioDao como ejemplo) con insertar/consultar/actualizar/eliminar.
 *
 * Tablas sugeridas para el resto del equipo (todas pueden referenciar usuarios(id)):
 *  - tours, reservas, favoritos, resenas, mensajes/conversaciones, metodos_pago, actividad_reciente.
 */
class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // Necesario para que funcionen las claves foráneas (FOREIGN KEY)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_USUARIOS)
        // TODO(equipo): db.execSQL(CREATE_TOURS), db.execSQL(CREATE_RESERVAS), ...
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Ejemplo de cómo agregar una tabla en la versión 2:
        // if (oldVersion < 2) { db.execSQL(CREATE_TOURS) }
    }

    companion object {
        const val DB_NAME = "pura_guia.db"
        const val DB_VERSION = 1

        // ---------- Tabla: usuarios ----------
        const val TABLA_USUARIOS = "usuarios"
        const val COL_ID = "id"
        const val COL_ROL = "rol"
        const val COL_NOMBRE = "nombre"
        const val COL_EMAIL = "email"
        const val COL_TELEFONO = "telefono"
        const val COL_PAIS = "pais"
        const val COL_INTERESES = "intereses"
        const val COL_IDIOMAS = "idiomas"
        const val COL_UBICACIONES = "ubicaciones"
        const val COL_CERTIFICACIONES = "certificaciones"
        const val COL_FOTO_PATH = "foto_path"
        const val COL_PASSWORD_HASH = "password_hash"
        const val COL_SALT = "salt"
        const val COL_FECHA_REGISTRO = "fecha_registro"

        private const val CREATE_USUARIOS =
            "CREATE TABLE usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "rol TEXT NOT NULL CHECK (rol IN ('TURISTA','GUIA')), " +
                "nombre TEXT NOT NULL, " +
                "email TEXT NOT NULL UNIQUE, " +
                "telefono TEXT NOT NULL, " +
                "pais TEXT, " +
                "intereses TEXT, " +
                "idiomas TEXT, " +
                "ubicaciones TEXT, " +
                "certificaciones TEXT, " +
                "foto_path TEXT, " +
                "password_hash TEXT NOT NULL, " +
                "salt TEXT NOT NULL, " +
                "fecha_registro INTEGER NOT NULL" +
                ")"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
    }
}
