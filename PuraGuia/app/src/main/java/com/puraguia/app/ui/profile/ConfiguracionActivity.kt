package com.puraguia.app.ui.profile

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.UsuarioDao
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.Validators
import com.puraguia.app.util.dp
import com.puraguia.app.util.setupHeader
import com.puraguia.app.util.toast

/**
 * Pantalla "Configuración": editar perfil, cambiar contraseña y eliminar cuenta (SQLite real),
 * más controles visuales para notificaciones, preferencias y textos legales.
 */
class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var usuarioDao: UsuarioDao
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)
        setupHeader("Configuración")

        usuarioDao = UsuarioDao(this)
        session = SessionManager(this)

        findViewById<View>(R.id.row_editar_perfil).setOnClickListener { mostrarDialogoEditarPerfil() }
        findViewById<View>(R.id.row_cambiar_password).setOnClickListener { mostrarDialogoPassword() }

        findViewById<View>(R.id.row_privacidad).setOnClickListener {
            mostrarInfoDialogo(
                "Política de Privacidad",
                "Tus datos de perfil y credenciales se almacenan únicamente de forma local en tu dispositivo mediante SQLite."
            )
        }
        findViewById<View>(R.id.row_terminos).setOnClickListener {
            mostrarInfoDialogo(
                "Términos y Condiciones",
                "Pura Guía conecta turistas con guías locales en Costa Rica. Respeta las indicaciones de seguridad de cada tour."
            )
        }

        findViewById<View>(R.id.btn_eliminar_cuenta).setOnClickListener { confirmarEliminarCuenta() }
    }

    private fun mostrarDialogoEditarPerfil() {
        val usuario = usuarioDao.obtenerPorId(session.getUserId())
        val editNombre = crearCampo("Nombre completo", usuario?.nombre ?: session.getNombre(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        val editTelefono = crearCampo("Teléfono", usuario?.telefono ?: "", InputType.TYPE_CLASS_PHONE)

        val contenedor = crearContenedorDialogo(
            "Nombre completo" to editNombre,
            "Teléfono" to editTelefono
        )

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Editar Perfil")
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nombre = editNombre.text.toString().trim()
                val telefono = editTelefono.text.toString().trim()
                var valido = true

                if (!Validators.esNombreValido(nombre)) {
                    editNombre.error = "Ingresa tu nombre completo"
                    valido = false
                }
                if (!Validators.esTelefonoValido(telefono)) {
                    editTelefono.error = "Ingresa un teléfono válido (8 a 15 dígitos)"
                    valido = false
                }
                if (!valido) return@setOnClickListener

                val userId = session.getUserId()
                if (usuarioDao.actualizarPerfil(userId, nombre, telefono)) {
                    usuarioDao.obtenerPorId(userId)?.let { session.guardarSesion(it) }
                    toast("Perfil actualizado")
                    dialogo.dismiss()
                } else {
                    toast("No se pudo actualizar el perfil")
                }
            }
        }
        dialogo.show()
    }

    private fun mostrarDialogoPassword() {
        val tipoPass = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val editActual = crearCampo("••••••••", "", tipoPass)
        val editNueva = crearCampo("••••••••", "", tipoPass)
        val editConfirmar = crearCampo("••••••••", "", tipoPass)

        val contenedor = crearContenedorDialogo(
            "Contraseña actual" to editActual,
            "Nueva contraseña" to editNueva,
            "Confirmar nueva contraseña" to editConfirmar
        )

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Cambiar Contraseña")
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Actualizar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val actual = editActual.text.toString()
                val nueva = editNueva.text.toString()
                val confirmar = editConfirmar.text.toString()
                var valido = true

                if (actual.isEmpty()) {
                    editActual.error = "Ingresa tu contraseña actual"
                    valido = false
                }
                if (!Validators.esPasswordValida(nueva)) {
                    editNueva.error = Validators.MSG_PASSWORD
                    valido = false
                }
                if (nueva != confirmar) {
                    editConfirmar.error = "Las contraseñas no coinciden"
                    valido = false
                }
                if (!valido) return@setOnClickListener

                if (usuarioDao.cambiarPassword(session.getUserId(), actual, nueva)) {
                    toast("Contraseña actualizada")
                    dialogo.dismiss()
                } else {
                    editActual.error = "La contraseña actual es incorrecta"
                }
            }
        }
        dialogo.show()
    }

    private fun confirmarEliminarCuenta() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Cuenta")
            .setMessage("¿Seguro que deseas eliminar tu cuenta? Esta acción es permanente y no se puede deshacer.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                val eliminada = usuarioDao.eliminar(session.getUserId())
                session.cerrarSesion()
                toast(if (eliminada) "Cuenta eliminada" else "Sesión cerrada")
                finish()
            }
            .show()
    }

    private fun mostrarInfoDialogo(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun crearCampo(hint: String, valorInicial: String, inputType: Int): EditText {
        return EditText(this).apply {
            this.hint = hint
            this.inputType = inputType
            setText(valorInicial)
            maxLines = 1
        }
    }

    private fun crearContenedorDialogo(vararg campos: Pair<String, EditText>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
            campos.forEachIndexed { index, (etiqueta, campo) ->
                val label = TextView(this@ConfiguracionActivity).apply {
                    text = etiqueta
                    textSize = 13f
                    if (index > 0) setPadding(0, dp(10), 0, 0)
                }
                addView(label)
                addView(campo)
            }
        }
    }
}
