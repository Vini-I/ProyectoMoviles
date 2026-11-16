package com.puraguia.app.util

import android.util.Patterns

object Validators {

    const val MSG_PASSWORD = "Mínimo 8 caracteres, con letras y números"

    fun esEmailValido(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    /** Acepta formatos como "+506 8888-8888": 8 a 15 dígitos. */
    fun esTelefonoValido(telefono: String): Boolean {
        val limpio = telefono.trim()
        val digitos = limpio.count { it.isDigit() }
        val soloCaracteresValidos = limpio.all { it.isDigit() || it == ' ' || it == '+' || it == '-' || it == '(' || it == ')' }
        return digitos in 8..15 && soloCaracteresValidos
    }

    fun esNombreValido(nombre: String): Boolean = nombre.trim().length >= 3

    fun esPasswordValida(password: String): Boolean =
        password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }
}
