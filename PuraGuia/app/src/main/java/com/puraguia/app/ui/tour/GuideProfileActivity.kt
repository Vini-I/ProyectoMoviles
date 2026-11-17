package com.puraguia.app.ui.tour

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.TourDAO
import com.puraguia.app.data.UsuarioDao
import com.puraguia.app.ui.chat.ChatActivity
import com.puraguia.app.ui.common.TourCardFactory
import com.puraguia.app.util.AuthGuard
import com.puraguia.app.util.FileUtils
import com.puraguia.app.util.iniciales
import com.puraguia.app.util.setupHeader

/**
 * Perfil público de un guía: idiomas, ubicaciones, certificaciones y todos sus tours desde SQLite.
 */
class GuideProfileActivity : AppCompatActivity() {

    private lateinit var usuarioDao: UsuarioDao
    private lateinit var tourDao: TourDAO

    private var guiaId: Long = -1L
    private var nombreGuia: String = "Carlos Rodríguez"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide_profile)
        setupHeader("Perfil del Guía")

        usuarioDao = UsuarioDao(this)
        tourDao = TourDAO(this)

        guiaId = intent.getLongExtra(EXTRA_GUIA_ID, -1L)
        nombreGuia = intent.getStringExtra(EXTRA_NOMBRE) ?: "Carlos Rodríguez"
    }

    override fun onResume() {
        super.onResume()
        cargarPerfil()
    }

    private fun cargarPerfil() {
        val guia = if (guiaId > 0L) {
            usuarioDao.obtenerPorId(guiaId)
        } else {
            usuarioDao.obtenerPorNombre(nombreGuia)
        }

        val nombreFinal = guia?.nombre ?: nombreGuia
        val idFinal = guia?.id ?: guiaId

        findViewById<TextView>(R.id.txt_iniciales).text = iniciales(nombreFinal)
        findViewById<TextView>(R.id.txt_nombre).text = nombreFinal

        val idiomas = guia?.idiomas?.ifEmpty { listOf("Español", "Inglés") } ?: listOf("Español", "Inglés")
        val ubicaciones = guia?.ubicaciones?.ifEmpty { listOf("Costa Rica") } ?: listOf("Costa Rica")
        val certs = guia?.certificaciones?.map {
            if (it.contains("/") || it.contains("\\")) FileUtils.nombreLegible(it) else it
        }?.ifEmpty { listOf("Guía local verificado en Pura Guía") }
            ?: listOf("Guía local verificado en Pura Guía")

        findViewById<TextView>(R.id.txt_bio).text =
            "Guía local en ${ubicaciones.joinToString(", ")}. Especialista en tours auténticos por Costa Rica."

        llenarTags(R.id.container_idiomas, idiomas)
        llenarTags(R.id.container_ubicaciones, ubicaciones)

        val certificaciones = findViewById<LinearLayout>(R.id.container_certificaciones)
        certificaciones.removeAllViews()
        certs.forEach { certificado ->
            val fila = layoutInflater.inflate(R.layout.item_certificado, certificaciones, false)
            fila.findViewById<TextView>(R.id.txt_nombre_archivo).text = certificado
            fila.findViewById<View>(R.id.btn_quitar_archivo).visibility = View.GONE
            certificaciones.addView(fila)
        }

        val toursGuia = if (idFinal > 0L) {
            tourDao.obtenerPorGuia(idFinal)
        } else {
            tourDao.obtenerPorNombreGuia(nombreFinal)
        }

        if (toursGuia.isNotEmpty()) {
            val totalResenas = toursGuia.sumOf { it.resenas }
            val prom = Math.round((toursGuia.map { it.rating }.average()) * 10.0) / 10.0
            findViewById<TextView>(R.id.txt_rating).text = "$prom ($totalResenas reseñas)"
        }

        findViewById<TextView>(R.id.txt_titulo_tours).text = "Tours de ${nombreFinal.substringBefore(' ')}"
        val contenedorTours = findViewById<LinearLayout>(R.id.container_tours)
        contenedorTours.removeAllViews()
        toursGuia.forEach { tour ->
            contenedorTours.addView(
                TourCardFactory.crearFila(layoutInflater, contenedorTours, tour) {
                    startActivity(TourDetailActivity.createIntent(this, it.id))
                }
            )
        }

        findViewById<View>(R.id.btn_mensaje).setOnClickListener {
            AuthGuard.requireLogin(this, "Inicia sesión para enviar mensajes") {
                startActivity(ChatActivity.createIntent(this, nombreFinal, true, idFinal))
            }
        }
    }

    private fun llenarTags(idContenedor: Int, textos: List<String>) {
        val contenedor = findViewById<LinearLayout>(idContenedor)
        contenedor.removeAllViews()
        textos.forEach { texto ->
            val tag = layoutInflater.inflate(R.layout.item_tag, contenedor, false) as TextView
            tag.text = texto
            contenedor.addView(tag)
        }
    }

    companion object {
        private const val EXTRA_NOMBRE = "guia_nombre"
        private const val EXTRA_GUIA_ID = "guia_id"

        fun createIntent(context: Context, nombreGuia: String, guiaId: Long = -1L): Intent =
            Intent(context, GuideProfileActivity::class.java)
                .putExtra(EXTRA_NOMBRE, nombreGuia)
                .putExtra(EXTRA_GUIA_ID, guiaId)
    }
}
