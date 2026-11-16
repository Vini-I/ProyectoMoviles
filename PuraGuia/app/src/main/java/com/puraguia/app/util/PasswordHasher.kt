package com.puraguia.app.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Encriptación de contraseñas: nunca se guarda la contraseña en texto plano.
 * Se guarda hash = SHA-256(salt + contraseña) y el salt (un valor aleatorio por usuario).
 */
object PasswordHasher {

    fun generarSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val resultado = digest.digest((salt + password).toByteArray(Charsets.UTF_8))
        return resultado.joinToString("") { byte -> "%02x".format(byte) }
    }
}
