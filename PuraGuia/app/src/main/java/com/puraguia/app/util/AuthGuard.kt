package com.puraguia.app.util

import android.content.Context
import android.widget.Toast
import com.puraguia.app.ui.auth.LoginActivity

/**
 * REGLA DE LA APP: se puede navegar sin iniciar sesión, pero para
 *  - reservar un tour
 *  - enviar un mensaje
 *  - guardar en favoritos
 *  - dejar una reseña
 *  - entrar a Perfil / Mensajes
 * hay que tener sesión. Todas esas acciones se envuelven así:
 *
 *   AuthGuard.requireLogin(this, "Inicia sesión para reservar") {
 *       // aquí va la lógica que SOLO se ejecuta si hay sesión
 *   }
 *
 * Si no hay sesión se abre la pantalla de login; al volver (ya con sesión) la persona
 * vuelve a tocar el botón y la acción se ejecuta.
 */
object AuthGuard {

    fun requireLogin(
        context: Context,
        mensaje: String = "Inicia sesión para continuar",
        accion: () -> Unit
    ) {
        if (SessionManager(context).isLoggedIn()) {
            accion()
        } else {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
            context.startActivity(LoginActivity.createIntent(context))
        }
    }
}
