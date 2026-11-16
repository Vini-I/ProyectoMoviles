package com.puraguia.app.util

import android.content.Context
import com.puraguia.app.data.Usuario

/**
 * Guarda quién tiene la sesión iniciada (SharedPreferences).
 * Úsenlo en cualquier pantalla para saber el usuario actual:
 *
 *   val session = SessionManager(this)
 *   if (session.isLoggedIn()) { val idUsuario = session.getUserId() ... }
 */
class SessionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun guardarSesion(usuario: Usuario) {
        prefs.edit()
            .putLong(KEY_USER_ID, usuario.id)
            .putString(KEY_NOMBRE, usuario.nombre)
            .putString(KEY_EMAIL, usuario.email)
            .putString(KEY_ROL, usuario.rol)
            .apply()
    }

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = prefs.getLong(KEY_USER_ID, NO_USER) != NO_USER

    /** id del usuario en la tabla `usuarios` (o -1 si no hay sesión). */
    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, NO_USER)

    fun getNombre(): String = prefs.getString(KEY_NOMBRE, "") ?: ""

    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""

    /** "TURISTA" o "GUIA" (ver Usuario.ROL_TURISTA / Usuario.ROL_GUIA). */
    fun getRol(): String = prefs.getString(KEY_ROL, "") ?: ""

    fun esGuia(): Boolean = getRol() == Usuario.ROL_GUIA

    fun esTurista(): Boolean = getRol() == Usuario.ROL_TURISTA

    companion object {
        private const val PREFS_NAME = "pura_guia_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NOMBRE = "nombre"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROL = "rol"
        private const val NO_USER = -1L
    }
}
