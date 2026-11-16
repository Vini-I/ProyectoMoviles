package com.puraguia.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.GridLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.Usuario
import com.puraguia.app.data.UsuarioDao
import com.puraguia.app.util.ChipGrid
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.Validators
import com.puraguia.app.util.toast

/** Registro de turista: crea la cuenta en SQLite e inicia sesión automáticamente. */
class RegisterTouristActivity : AppCompatActivity() {

    private lateinit var usuarioDao: UsuarioDao
    private lateinit var session: SessionManager

    private lateinit var editNombre: EditText
    private lateinit var editEmail: EditText
    private lateinit var editTelefono: EditText
    private lateinit var spinnerPais: Spinner
    private lateinit var editPassword: EditText
    private lateinit var editConfirmar: EditText
    private lateinit var chipsIntereses: ChipGrid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_tourist)

        usuarioDao = UsuarioDao(this)
        session = SessionManager(this)

        editNombre = findViewById(R.id.edit_nombre)
        editEmail = findViewById(R.id.edit_email)
        editTelefono = findViewById(R.id.edit_telefono)
        spinnerPais = findViewById(R.id.spinner_pais)
        editPassword = findViewById(R.id.edit_password)
        editConfirmar = findViewById(R.id.edit_confirmar)

        val adaptador = ArrayAdapter.createFromResource(
            this, R.array.paises, android.R.layout.simple_spinner_item
        )
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPais.adapter = adaptador

        chipsIntereses = ChipGrid(
            this,
            findViewById<GridLayout>(R.id.grid_intereses),
            listOf("Aventura", "Naturaleza", "Playa", "Cultura", "Gastronomía", "Fotografía")
        )

        findViewById<View>(R.id.btn_crear_cuenta).setOnClickListener { registrar() }
        findViewById<View>(R.id.btn_volver).setOnClickListener { finish() }
    }

    private fun registrar() {
        val nombre = editNombre.text.toString().trim()
        val email = editEmail.text.toString().trim()
        val telefono = editTelefono.text.toString().trim()
        val password = editPassword.text.toString()
        val confirmar = editConfirmar.text.toString()
        val intereses = chipsIntereses.seleccionados
        val paisSeleccionado = spinnerPais.selectedItemPosition > 0

        var valido = true
        if (!Validators.esNombreValido(nombre)) {
            editNombre.error = "Ingresa tu nombre completo"
            valido = false
        }
        if (!Validators.esEmailValido(email)) {
            editEmail.error = "Ingresa un correo válido"
            valido = false
        }
        if (!Validators.esTelefonoValido(telefono)) {
            editTelefono.error = "Ingresa un teléfono válido (8 a 15 dígitos)"
            valido = false
        }
        if (!Validators.esPasswordValida(password)) {
            editPassword.error = Validators.MSG_PASSWORD
            valido = false
        }
        if (password != confirmar) {
            editConfirmar.error = "Las contraseñas no coinciden"
            valido = false
        }
        if (!paisSeleccionado) {
            toast("Selecciona tu país")
            valido = false
        } else if (intereses.isEmpty()) {
            toast("Selecciona al menos un interés")
            valido = false
        }
        if (!valido) return

        try {
            if (usuarioDao.existeEmail(email)) {
                editEmail.error = "Este correo ya está registrado"
                return
            }
            val nuevo = Usuario(
                rol = Usuario.ROL_TURISTA,
                nombre = nombre,
                email = email,
                telefono = telefono,
                pais = spinnerPais.selectedItem.toString(),
                intereses = intereses
            )
            val id = usuarioDao.insertar(nuevo, password)
            if (id <= 0) {
                toast("No se pudo crear la cuenta. Intenta nuevamente.")
                return
            }
            session.guardarSesion(nuevo.copy(id = id, email = email.lowercase()))
            toast("¡Cuenta creada! Bienvenido a Pura Guía")
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            toast("Ocurrió un error al guardar tus datos.")
        }
    }
}
