package com.puraguia.app.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.MensajeriaDao
import com.puraguia.app.data.UsuarioDao
import com.puraguia.app.model.Mensaje
import com.puraguia.app.ui.tour.GuideProfileActivity
import com.puraguia.app.util.AuthGuard
import com.puraguia.app.util.FileUtils
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.iniciales
import com.puraguia.app.util.toast
import java.io.File

/**
 * Conversación con un guía o turista persistida en SQLite (`conversaciones` y `mensajes`).
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var mensajeriaDao: MensajeriaDao
    private lateinit var usuarioDao: UsuarioDao
    private lateinit var session: SessionManager

    private lateinit var contenedorMensajes: LinearLayout
    private lateinit var scrollChat: ScrollView
    private lateinit var editMensaje: EditText
    private lateinit var nombreContacto: String

    private var contactoId: Long = -1L
    private var conversacionId: Long = -1L

    private val adjuntarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        AuthGuard.requireLogin(this, "Inicia sesión para enviar archivos") {
            val ruta = FileUtils.copiarAInterno(this, uri, "adjuntos")
            if (ruta == null) {
                toast("No se pudo adjuntar el archivo")
                return@requireLogin
            }
            val nombreArchivo = FileUtils.nombreLegible(ruta)
            asegurarConversacion()
            if (conversacionId > 0L) {
                val idMsg = mensajeriaDao.enviarMensaje(
                    conversacionId = conversacionId,
                    remitenteId = session.getUserId(),
                    contenido = "📎 $nombreArchivo",
                    tipo = Mensaje.TIPO_ADJUNTO,
                    adjuntoPath = ruta
                )
                if (idMsg > 0L) {
                    cargarMensajes()
                } else {
                    toast("No se pudo guardar el archivo en el chat")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        mensajeriaDao = MensajeriaDao(this)
        usuarioDao = UsuarioDao(this)
        session = SessionManager(this)

        nombreContacto = intent.getStringExtra(EXTRA_NOMBRE) ?: "Guía Pura Guía"
        contactoId = intent.getLongExtra(EXTRA_CONTACTO_ID, -1L)
        conversacionId = intent.getLongExtra(EXTRA_CONVERSACION_ID, -1L)
        val enLinea = intent.getBooleanExtra(EXTRA_EN_LINEA, true)

        if (contactoId <= 0L) {
            val encontrado = usuarioDao.obtenerPorNombre(nombreContacto)
            if (encontrado != null) {
                contactoId = encontrado.id
                nombreContacto = encontrado.nombre
            }
        } else {
            usuarioDao.obtenerPorId(contactoId)?.let { nombreContacto = it.nombre }
        }

        contenedorMensajes = findViewById(R.id.container_mensajes)
        scrollChat = findViewById(R.id.scroll_chat)
        editMensaje = findViewById(R.id.edit_mensaje)

        findViewById<TextView>(R.id.txt_iniciales_header).text = iniciales(nombreContacto)
        findViewById<TextView>(R.id.txt_nombre_header).text = nombreContacto
        findViewById<TextView>(R.id.txt_estado_header).text = if (enLinea) "En línea" else "Desconectado"
        findViewById<View>(R.id.dot_online).visibility = if (enLinea) View.VISIBLE else View.GONE

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.header_info).setOnClickListener {
            startActivity(GuideProfileActivity.createIntent(this, nombreContacto, contactoId))
        }
        findViewById<View>(R.id.btn_adjuntar).setOnClickListener {
            AuthGuard.requireLogin(this, "Inicia sesión para adjuntar archivos") {
                adjuntarLauncher.launch("*/*")
            }
        }
        findViewById<View>(R.id.btn_enviar).setOnClickListener { enviar() }

        if (session.isLoggedIn()) {
            asegurarConversacion()
            if (conversacionId > 0L) {
                mensajeriaDao.marcarComoLeidos(conversacionId, session.getUserId())
                cargarMensajes()
            }
        }
    }

    private fun asegurarConversacion() {
        if (conversacionId > 0L || !session.isLoggedIn()) return
        if (contactoId <= 0L) {
            contactoId = usuarioDao.obtenerGuias().firstOrNull()?.id ?: -1L
        }
        if (contactoId > 0L) {
            conversacionId = mensajeriaDao.obtenerOCrearConversacion(session.getUserId(), contactoId)
        }
    }

    private fun cargarMensajes() {
        // Conservamos el primer hijo ("Hoy") y reconstruimos las burbujas desde SQLite
        while (contenedorMensajes.childCount > 1) {
            contenedorMensajes.removeViewAt(1)
        }
        val mensajes = mensajeriaDao.obtenerMensajes(conversacionId, session.getUserId())
        mensajes.forEach { agregarBurbuja(it) }
    }

    private fun enviar() {
        val texto = editMensaje.text.toString().trim()
        if (texto.isEmpty()) return
        AuthGuard.requireLogin(this, "Inicia sesión para enviar mensajes") {
            asegurarConversacion()
            if (conversacionId <= 0L) {
                toast("No se pudo iniciar la conversación con este contacto")
                return@requireLogin
            }
            val id = mensajeriaDao.enviarMensaje(
                conversacionId = conversacionId,
                remitenteId = session.getUserId(),
                contenido = texto
            )
            if (id > 0L) {
                editMensaje.text.clear()
                cargarMensajes()
            } else {
                toast("Error al enviar el mensaje")
            }
        }
    }

    private fun agregarBurbuja(mensaje: Mensaje) {
        val layout = if (mensaje.esMio) R.layout.item_bubble_out else R.layout.item_bubble_in
        val burbuja = layoutInflater.inflate(layout, contenedorMensajes, false)
        val txtMensaje = burbuja.findViewById<TextView>(R.id.txt_mensaje)
        txtMensaje.text = mensaje.texto
        burbuja.findViewById<TextView>(R.id.txt_hora).text = mensaje.hora

        if (mensaje.tipo == Mensaje.TIPO_ADJUNTO && !mensaje.adjuntoPath.isNullOrBlank()) {
            txtMensaje.setOnClickListener { mostrarAdjunto(mensaje.adjuntoPath) }
        }

        if (mensaje.esMio && mensaje.id > 0L) {
            txtMensaje.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Eliminar mensaje")
                    .setMessage("¿Deseas eliminar este mensaje?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Eliminar") { _, _ ->
                        if (mensajeriaDao.eliminarMensaje(mensaje.id, conversacionId)) {
                            cargarMensajes()
                        }
                    }
                    .show()
                true
            }
        }

        contenedorMensajes.addView(burbuja)
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun mostrarAdjunto(ruta: String) {
        val archivo = File(ruta)
        if (!archivo.exists()) {
            toast("El archivo adjunto no está disponible")
            return
        }
        val nombre = FileUtils.nombreLegible(ruta)
        val esImagen = nombre.lowercase().let {
            it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".webp")
        }
        if (esImagen) {
            val img = ImageView(this).apply {
                setImageURI(Uri.fromFile(archivo))
                adjustViewBounds = true
                setPadding(24, 24, 24, 24)
            }
            AlertDialog.Builder(this)
                .setTitle(nombre)
                .setView(img)
                .setPositiveButton("Cerrar", null)
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Archivo adjunto")
                .setMessage("Archivo guardado localmente:\n$nombre")
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    companion object {
        private const val EXTRA_NOMBRE = "contacto_nombre"
        private const val EXTRA_EN_LINEA = "contacto_en_linea"
        private const val EXTRA_CONTACTO_ID = "contacto_id"
        private const val EXTRA_CONVERSACION_ID = "conversacion_id"

        fun createIntent(
            context: Context,
            nombre: String,
            enLinea: Boolean,
            contactoId: Long = -1L,
            conversacionId: Long = -1L
        ): Intent =
            Intent(context, ChatActivity::class.java)
                .putExtra(EXTRA_NOMBRE, nombre)
                .putExtra(EXTRA_EN_LINEA, enLinea)
                .putExtra(EXTRA_CONTACTO_ID, contactoId)
                .putExtra(EXTRA_CONVERSACION_ID, conversacionId)
    }
}
