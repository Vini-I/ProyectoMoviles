package com.puraguia.app.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.UsuarioDao
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.Validators
import com.puraguia.app.util.toast

/**
 * Pantalla de inicio de sesión.
 *
 * Resultado: si el login (o el registro que se hace desde aquí) sale bien, devuelve RESULT_OK
 * y se cierra, para que la pantalla que la abrió continúe (quedando con sesión iniciada).
 * Si el usuario toca "atrás" sigue como invitado (RESULT_CANCELED).
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var usuarioDao: UsuarioDao
    private lateinit var session: SessionManager
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText

    // Si el usuario se registró correctamente, la sesión ya quedó iniciada: cerramos el login.
    private val registroLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usuarioDao = UsuarioDao(this)
        session = SessionManager(this)
        editEmail = findViewById(R.id.edit_email)
        editPassword = findViewById(R.id.edit_password)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_login).setOnClickListener { iniciarSesion() }
        findViewById<View>(R.id.btn_registrarse).setOnClickListener {
            registroLauncher.launch(Intent(this, RegisterRoleActivity::class.java))
        }
        // Google / Facebook: solo diseño (el proyecto no usa servicios externos)
        findViewById<View>(R.id.btn_google).setOnClickListener { toast("Inicio con Google: próximamente") }
        findViewById<View>(R.id.btn_facebook).setOnClickListener { toast("Inicio con Facebook: próximamente") }
    }

    private fun iniciarSesion() {
        val email = editEmail.text.toString().trim()
        val password = editPassword.text.toString()

        var valido = true
        if (!Validators.esEmailValido(email)) {
            editEmail.error = "Ingresa un correo válido"
            valido = false
        }
        if (password.isEmpty()) {
            editPassword.error = "Ingresa tu contraseña"
            valido = false
        }
        if (!valido) return

        try {
            val usuario = usuarioDao.autenticar(email, password)
            if (usuario == null) {
                toast("Correo o contraseña incorrectos")
            } else {
                session.guardarSesion(usuario)
                toast("¡Bienvenido, ${usuario.nombre.substringBefore(' ')}!")
                setResult(RESULT_OK)
                finish()
            }
        } catch (e: Exception) {
            toast("No se pudo iniciar sesión. Intenta nuevamente.")
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, LoginActivity::class.java)
    }
}
