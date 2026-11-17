package com.puraguia.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import com.puraguia.app.util.PasswordHasher

/**
 * DAO (Data Access Object) de usuarios: aquí viven TODAS las consultas SQL de la tabla `usuarios`.
 * Las pantallas nunca escriben SQL; llaman a estos métodos.
 *
 * Este DAO sirve como ejemplo para que el resto del equipo cree los suyos (TourDao, ReservaDao...).
 */
class UsuarioDao(context: Context) {

    private val helper = DatabaseHelper.getInstance(context)

    /** Inserta un usuario nuevo. Devuelve el id generado, o -1 si hubo un error. */
    fun insertar(usuario: Usuario, password: String): Long {
        val salt = PasswordHasher.generarSalt()
        val valores = ContentValues().apply {
            put(DatabaseHelper.COL_ROL, usuario.rol)
            put(DatabaseHelper.COL_NOMBRE, usuario.nombre.trim())
            put(DatabaseHelper.COL_EMAIL, normalizarEmail(usuario.email))
            put(DatabaseHelper.COL_TELEFONO, usuario.telefono.trim())
            put(DatabaseHelper.COL_PAIS, usuario.pais)
            put(DatabaseHelper.COL_INTERESES, unir(usuario.intereses))
            put(DatabaseHelper.COL_IDIOMAS, unir(usuario.idiomas))
            put(DatabaseHelper.COL_UBICACIONES, unir(usuario.ubicaciones))
            put(DatabaseHelper.COL_CERTIFICACIONES, unir(usuario.certificaciones))
            put(DatabaseHelper.COL_FOTO_PATH, usuario.fotoPath)
            put(DatabaseHelper.COL_PASSWORD_HASH, PasswordHasher.hash(password, salt))
            put(DatabaseHelper.COL_SALT, salt)
            put(DatabaseHelper.COL_FECHA_REGISTRO, usuario.fechaRegistro)
        }
        return try {
            helper.writableDatabase.insertOrThrow(DatabaseHelper.TABLA_USUARIOS, null, valores)
        } catch (e: SQLiteException) {
            -1L
        }
    }

    /** true si ya existe un usuario con ese correo. */
    fun existeEmail(email: String): Boolean {
        return helper.readableDatabase.query(
            DatabaseHelper.TABLA_USUARIOS,
            arrayOf(DatabaseHelper.COL_ID),
            "${DatabaseHelper.COL_EMAIL} = ?",
            arrayOf(normalizarEmail(email)),
            null, null, null
        ).use { cursor -> cursor.moveToFirst() }
    }

    /** Devuelve el usuario si el correo y la contraseña son correctos; si no, null. */
    fun autenticar(email: String, password: String): Usuario? {
        return helper.readableDatabase.query(
            DatabaseHelper.TABLA_USUARIOS,
            null,
            "${DatabaseHelper.COL_EMAIL} = ?",
            arrayOf(normalizarEmail(email)),
            null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                val salt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SALT))
                val hashGuardado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PASSWORD_HASH))
                if (PasswordHasher.hash(password, salt) == hashGuardado) cursorAUsuario(cursor) else null
            }
        }
    }

    /** Busca un usuario por su id (por ejemplo, el de la sesión actual). */
    fun obtenerPorId(id: Long): Usuario? {
        return helper.readableDatabase.query(
            DatabaseHelper.TABLA_USUARIOS,
            null,
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        ).use { cursor -> if (cursor.moveToFirst()) cursorAUsuario(cursor) else null }
    }

    /** Busca un usuario por su nombre exacto (o coincidencia parcial sin distinguir mayúsculas). */
    fun obtenerPorNombre(nombre: String): Usuario? {
        val limpio = nombre.trim()
        helper.readableDatabase.query(
            DatabaseHelper.TABLA_USUARIOS,
            null,
            "LOWER(${DatabaseHelper.COL_NOMBRE}) = LOWER(?)",
            arrayOf(limpio),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) return cursorAUsuario(cursor)
        }
        return helper.readableDatabase.query(
            DatabaseHelper.TABLA_USUARIOS,
            null,
            "LOWER(${DatabaseHelper.COL_NOMBRE}) LIKE LOWER(?)",
            arrayOf("%$limpio%"),
            null, null, null
        ).use { cursor -> if (cursor.moveToFirst()) cursorAUsuario(cursor) else null }
    }

    /** Devuelve todos los guías registrados en la base de datos. */
    fun obtenerGuias(): List<Usuario> {
        val lista = mutableListOf<Usuario>()
        helper.readableDatabase.query(
            DatabaseHelper.TABLA_USUARIOS,
            null,
            "${DatabaseHelper.COL_ROL} = ?",
            arrayOf(Usuario.ROL_GUIA),
            null, null, "${DatabaseHelper.COL_NOMBRE} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                lista.add(cursorAUsuario(cursor))
            }
        }
        return lista
    }

    /** Devuelve otros usuarios registrados (excluyendo el usuario actual) para iniciar conversaciones. */
    fun obtenerContactosDisponibles(excluirUsuarioId: Long): List<Usuario> {
        val lista = mutableListOf<Usuario>()
        helper.readableDatabase.query(
            DatabaseHelper.TABLA_USUARIOS,
            null,
            "${DatabaseHelper.COL_ID} != ?",
            arrayOf(excluirUsuarioId.toString()),
            null, null, "${DatabaseHelper.COL_ROL} ASC, ${DatabaseHelper.COL_NOMBRE} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                lista.add(cursorAUsuario(cursor))
            }
        }
        return lista
    }

    /** Elimina el usuario. Devuelve true si se eliminó. */
    fun eliminar(id: Long): Boolean {
        val filas = helper.writableDatabase.delete(
            DatabaseHelper.TABLA_USUARIOS,
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(id.toString())
        )
        return filas > 0
    }

    /** Actualiza nombre y teléfono del usuario. Devuelve true si se modificó el registro. */
    fun actualizarPerfil(id: Long, nombre: String, telefono: String): Boolean {
        val valores = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE, nombre.trim())
            put(DatabaseHelper.COL_TELEFONO, telefono.trim())
        }
        return try {
            val filas = helper.writableDatabase.update(
                DatabaseHelper.TABLA_USUARIOS,
                valores,
                "${DatabaseHelper.COL_ID} = ?",
                arrayOf(id.toString())
            )
            filas > 0
        } catch (e: SQLiteException) {
            false
        }
    }

    /**
     * Actualiza los datos específicos del guía.
     */
    fun actualizarConfiguracionGuia(
        id: Long,
        pais: String,
        idiomas: List<String>,
        ubicaciones: List<String>,
        certificaciones: List<String>
    ): Boolean {
        val valores = ContentValues().apply {
            put(DatabaseHelper.COL_PAIS, pais.trim())
            put(DatabaseHelper.COL_IDIOMAS, unir(idiomas))
            put(DatabaseHelper.COL_UBICACIONES, unir(ubicaciones))
            put(DatabaseHelper.COL_CERTIFICACIONES, unir(certificaciones))
        }

        return try {
            val filas = helper.writableDatabase.update(
                DatabaseHelper.TABLA_USUARIOS,
                valores,
                "${DatabaseHelper.COL_ID} = ?",
                arrayOf(id.toString())
            )
            filas > 0
        } catch (e: SQLiteException) {
            false
        }
    }

    /**
     * Verifica la contraseña actual y, si coincide, guarda una nueva contraseña con un salt nuevo.
     * Devuelve true si se actualizó correctamente.
     */
    fun cambiarPassword(id: Long, passwordActual: String, passwordNueva: String): Boolean {
        val esActualValida = helper.readableDatabase.query(
            DatabaseHelper.TABLA_USUARIOS,
            arrayOf(DatabaseHelper.COL_PASSWORD_HASH, DatabaseHelper.COL_SALT),
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                false
            } else {
                val saltActual = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SALT))
                val hashActual = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PASSWORD_HASH))
                PasswordHasher.hash(passwordActual, saltActual) == hashActual
            }
        }
        if (!esActualValida) return false

        val nuevoSalt = PasswordHasher.generarSalt()
        val valores = ContentValues().apply {
            put(DatabaseHelper.COL_PASSWORD_HASH, PasswordHasher.hash(passwordNueva, nuevoSalt))
            put(DatabaseHelper.COL_SALT, nuevoSalt)
        }
        return try {
            val filas = helper.writableDatabase.update(
                DatabaseHelper.TABLA_USUARIOS,
                valores,
                "${DatabaseHelper.COL_ID} = ?",
                arrayOf(id.toString())
            )
            filas > 0
        } catch (e: SQLiteException) {
            false
        }
    }

    // ---------- Utilidades privadas ----------

    private fun cursorAUsuario(c: Cursor): Usuario = Usuario(
        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
        rol = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROL)),
        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE)),
        email = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EMAIL)),
        telefono = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TELEFONO)),
        pais = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_PAIS)) ?: "",
        intereses = separar(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INTERESES))),
        idiomas = separar(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_IDIOMAS))),
        ubicaciones = separar(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_UBICACIONES))),
        certificaciones = separar(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CERTIFICACIONES))),
        fotoPath = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_FOTO_PATH)),
        fechaRegistro = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_FECHA_REGISTRO))
    )

    private fun normalizarEmail(email: String): String = email.trim().lowercase()

    private fun unir(lista: List<String>): String = lista.joinToString(SEPARADOR)

    private fun separar(texto: String?): List<String> =
        if (texto.isNullOrBlank()) emptyList() else texto.split(SEPARADOR)

    private companion object {
        const val SEPARADOR = "|"
    }
}
