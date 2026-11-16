package com.puraguia.app.ui.tour

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.SampleData
import com.puraguia.app.ui.chat.ChatActivity
import com.puraguia.app.ui.common.TourCardFactory
import com.puraguia.app.util.AuthGuard
import com.puraguia.app.util.iniciales
import com.puraguia.app.util.setupHeader

/**
 * Perfil público de un guía: idiomas, ubicaciones, certificaciones y todos sus tours.
 * Se llega desde el detalle de un tour o desde el chat.
 *
 * TODO(equipo guías): cargar el guía real (tabla usuarios con rol GUIA: idiomas, ubicaciones,
 * certificaciones ya se guardan en el registro) y sus tours desde SQLite.
 */
class GuideProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide_profile)
        setupHeader("Perfil del Guía")

        val nombre = intent.getStringExtra(EXTRA_NOMBRE) ?: SampleData.GUIA_NOMBRE

        findViewById<TextView>(R.id.txt_iniciales).text = iniciales(nombre)
        findViewById<TextView>(R.id.txt_nombre).text = nombre
        findViewById<TextView>(R.id.txt_bio).text = SampleData.GUIA_BIO

        llenarTags(R.id.container_idiomas, SampleData.guiaIdiomas)
        llenarTags(R.id.container_ubicaciones, SampleData.guiaUbicaciones)

        val certificaciones = findViewById<LinearLayout>(R.id.container_certificaciones)
        SampleData.guiaCertificaciones.forEach { certificado ->
            val fila = layoutInflater.inflate(R.layout.item_certificado, certificaciones, false)
            fila.findViewById<TextView>(R.id.txt_nombre_archivo).text = certificado
            fila.findViewById<View>(R.id.btn_quitar_archivo).visibility = View.GONE
            certificaciones.addView(fila)
        }

        // Tours del guía (si no tiene en los datos de ejemplo, se muestran los primeros)
        val toursGuia = SampleData.tours.filter { it.guia == nombre }.ifEmpty { SampleData.tours.take(2) }
        findViewById<TextView>(R.id.txt_titulo_tours).text = "Tours de ${nombre.substringBefore(' ')}"
        val contenedorTours = findViewById<LinearLayout>(R.id.container_tours)
        toursGuia.forEach { tour ->
            contenedorTours.addView(
                TourCardFactory.crearFila(layoutInflater, contenedorTours, tour) {
                    startActivity(TourDetailActivity.createIntent(this, it.id))
                }
            )
        }

        findViewById<View>(R.id.btn_mensaje).setOnClickListener {
            AuthGuard.requireLogin(this, "Inicia sesión para enviar mensajes") {
                startActivity(ChatActivity.createIntent(this, nombre, true))
            }
        }
    }

    private fun llenarTags(idContenedor: Int, textos: List<String>) {
        val contenedor = findViewById<LinearLayout>(idContenedor)
        textos.forEach { texto ->
            val tag = layoutInflater.inflate(R.layout.item_tag, contenedor, false) as TextView
            tag.text = texto
            contenedor.addView(tag)
        }
    }

    companion object {
        private const val EXTRA_NOMBRE = "guia_nombre"

        fun createIntent(context: Context, nombreGuia: String): Intent =
            Intent(context, GuideProfileActivity::class.java).putExtra(EXTRA_NOMBRE, nombreGuia)
    }
}
