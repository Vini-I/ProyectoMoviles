package com.puraguia.app.ui.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.Usuario
import com.puraguia.app.data.UsuarioDao
import com.puraguia.app.util.ChipGrid
import com.puraguia.app.util.FileUtils
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.Validators
import com.puraguia.app.util.setupHeader
import com.puraguia.app.util.toast
import java.io.File

/** Registro de guía: datos básicos, idiomas, ubicaciones, foto y certificaciones. */
class RegisterGuideActivity : AppCompatActivity() {

    private lateinit var usuarioDao: UsuarioDao
    private lateinit var session: SessionManager

    private lateinit var imgFoto: ImageView
    private lateinit var editNombre: EditText
    private lateinit var editEmail: EditText
    private lateinit var editTelefono: EditText
    private lateinit var editPassword: EditText
    private lateinit var editConfirmar: EditText
    private lateinit var chipsIdiomas: ChipGrid
    private lateinit var chipsUbicaciones: ChipGrid
    private lateinit var containerCertificados: LinearLayout

    private var fotoPath: String? = null
    private val certificados = mutableListOf<String>()

    // Selector de foto (no requiere permisos)
    private val fotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val ruta = FileUtils.copiarAInterno(this, uri, "fotos")
            if (ruta != null) {
                fotoPath = ruta
                imgFoto.imageTintList = null
                imgFoto.setPadding(0, 0, 0, 0)
                imgFoto.scaleType = ImageView.ScaleType.CENTER_CROP
                imgFoto.setImageURI(Uri.fromFile(File(ruta)))
            } else {
                toast("No se pudo cargar la foto")
            }
        }
    }

    // Selector de certificados (varios archivos)
    private val certificadosLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val ruta = FileUtils.copiarAInterno(this, uri, "certificados")
            if (ruta != null) certificados.add(ruta) else toast("No se pudo cargar un archivo")
        }
        mostrarCertificados()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_guide)
        setupHeader("Registro de Guía", "Completa tu perfil para comenzar a ofrecer tours")

        usuarioDao = UsuarioDao(this)
        session = SessionManager(this)

        imgFoto = findViewById(R.id.img_foto)
        editNombre = findViewById(R.id.edit_nombre)
        editEmail = findViewById(R.id.edit_email)
        editTelefono = findViewById(R.id.edit_telefono)
        editPassword = findViewById(R.id.edit_password)
        editConfirmar = findViewById(R.id.edit_confirmar)
        containerCertificados = findViewById(R.id.container_certificados)

        chipsIdiomas = ChipGrid(
            this,
            findViewById<GridLayout>(R.id.grid_idiomas),
            listOf("Español", "Inglés", "Francés", "Alemán", "Italiano", "Portugués", "Mandarín")
        )
        chipsUbicaciones = ChipGrid(
            this,
            findViewById<GridLayout>(R.id.grid_ubicaciones),
            listOf("San José", "Alajuela", "Cartago", "Heredia", "Guanacaste", "Puntarenas", "Limón")
        )

        findViewById<View>(R.id.btn_subir_foto).setOnClickListener { fotoLauncher.launch("image/*") }
        findViewById<View>(R.id.btn_subir_certificado).setOnClickListener { certificadosLauncher.launch("*/*") }
        findViewById<View>(R.id.btn_cancelar).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_registrar).setOnClickListener { registrar() }
    }

    private fun mostrarCertificados() {
        containerCertificados.removeAllViews()
        val inflater = LayoutInflater.from(this)
        certificados.toList().forEach { ruta ->
            val fila = inflater.inflate(R.layout.item_certificado, containerCertificados, false)
            fila.findViewById<TextView>(R.id.txt_nombre_archivo).text = FileUtils.nombreLegible(ruta)
            fila.findViewById<ImageButton>(R.id.btn_quitar_archivo).setOnClickListener {
                certificados.remove(ruta)
                mostrarCertificados()
            }
            containerCertificados.addView(fila)
        }
    }

    private fun registrar() {
        val nombre = editNombre.text.toString().trim()
        val email = editEmail.text.toString().trim()
        val telefono = editTelefono.text.toString().trim()
        val password = editPassword.text.toString()
        val confirmar = editConfirmar.text.toString()
        val idiomas = chipsIdiomas.seleccionados
        val ubicaciones = chipsUbicaciones.seleccionados

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
        if (idiomas.isEmpty()) {
            toast("Selecciona al menos un idioma")
            valido = false
        } else if (ubicaciones.isEmpty()) {
            toast("Selecciona al menos una ubicación")
            valido = false
        }
        if (!valido) return

        try {
            if (usuarioDao.existeEmail(email)) {
                editEmail.error = "Este correo ya está registrado"
                return
            }
            val nuevo = Usuario(
                rol = Usuario.ROL_GUIA,
                nombre = nombre,
                email = email,
                telefono = telefono,
                idiomas = idiomas,
                ubicaciones = ubicaciones,
                certificaciones = certificados.toList(),
                fotoPath = fotoPath
            )
            val id = usuarioDao.insertar(nuevo, password)
            if (id <= 0) {
                toast("No se pudo crear la cuenta. Intenta nuevamente.")
                return
            }
            session.guardarSesion(nuevo.copy(id = id, email = email.lowercase()))
            com.puraguia.app.data.ActividadDao(this).registrar(
                id,
                com.puraguia.app.model.TipoActividad.SESION,
                "Cuenta de guía creada",
                "Registro en Pura Guía"
            )
            com.puraguia.app.data.MensajeriaDao(this)
                .crearMensajeBienvenidaSiNuevo(id, esGuia = true)
            toast("¡Registro de guía completado! Bienvenido a Pura Guía")
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            toast("Ocurrió un error al guardar tus datos.")
        }
    }
}
