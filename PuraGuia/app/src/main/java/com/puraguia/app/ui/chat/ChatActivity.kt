package com.puraguia.app.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.SampleData
import com.puraguia.app.model.Mensaje
import com.puraguia.app.ui.tour.GuideProfileActivity
import com.puraguia.app.util.AuthGuard
import com.puraguia.app.util.iniciales
import com.puraguia.app.util.toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Conversación con un guía/turista. Tocar el encabezado abre el perfil del guía.
 *
 * TODO(equipo mensajería): cargar los mensajes de SQLite (tabla mensajes) y guardar los nuevos.
 * Hoy los mensajes iniciales son de ejemplo y los que se escriben solo se muestran en pantalla.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var contenedorMensajes: LinearLayout
    private lateinit var scrollChat: ScrollView
    private lateinit var editMensaje: EditText
    private lateinit var nombreContacto: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        nombreContacto = intent.getStringExtra(EXTRA_NOMBRE) ?: SampleData.GUIA_NOMBRE
        val enLinea = intent.getBooleanExtra(EXTRA_EN_LINEA, false)

        contenedorMensajes = findViewById(R.id.container_mensajes)
        scrollChat = findViewById(R.id.scroll_chat)
        editMensaje = findViewById(R.id.edit_mensaje)

        findViewById<TextView>(R.id.txt_iniciales_header).text = iniciales(nombreContacto)
        findViewById<TextView>(R.id.txt_nombre_header).text = nombreContacto
        findViewById<TextView>(R.id.txt_estado_header).text = if (enLinea) "En línea" else "Desconectado"
        findViewById<View>(R.id.dot_online).visibility = if (enLinea) View.VISIBLE else View.GONE

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.header_info).setOnClickListener {
            startActivity(GuideProfileActivity.createIntent(this, nombreContacto))
        }
        findViewById<View>(R.id.btn_adjuntar).setOnClickListener {
            toast("Adjuntar archivos: pendiente (equipo mensajería)")
        }
        findViewById<View>(R.id.btn_enviar).setOnClickListener { enviar() }

        SampleData.mensajes.forEach { agregarBurbuja(it) }
    }

    private fun enviar() {
        val texto = editMensaje.text.toString().trim()
        if (texto.isEmpty()) return
        AuthGuard.requireLogin(this, "Inicia sesión para enviar mensajes") {
            // TODO(equipo mensajería): guardar el mensaje en SQLite antes de mostrarlo
            val hora = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            agregarBurbuja(Mensaje(texto, hora, esMio = true))
            editMensaje.text.clear()
        }
    }

    private fun agregarBurbuja(mensaje: Mensaje) {
        val layout = if (mensaje.esMio) R.layout.item_bubble_out else R.layout.item_bubble_in
        val burbuja = layoutInflater.inflate(layout, contenedorMensajes, false)
        burbuja.findViewById<TextView>(R.id.txt_mensaje).text = mensaje.texto
        burbuja.findViewById<TextView>(R.id.txt_hora).text = mensaje.hora
        contenedorMensajes.addView(burbuja)
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    companion object {
        private const val EXTRA_NOMBRE = "contacto_nombre"
        private const val EXTRA_EN_LINEA = "contacto_en_linea"

        fun createIntent(context: Context, nombre: String, enLinea: Boolean): Intent =
            Intent(context, ChatActivity::class.java)
                .putExtra(EXTRA_NOMBRE, nombre)
                .putExtra(EXTRA_EN_LINEA, enLinea)
    }
}
