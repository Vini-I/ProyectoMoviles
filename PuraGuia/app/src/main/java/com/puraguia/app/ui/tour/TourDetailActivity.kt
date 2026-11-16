package com.puraguia.app.ui.tour

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.SampleData
import com.puraguia.app.model.Tour
import com.puraguia.app.ui.chat.ChatActivity
import com.puraguia.app.util.AuthGuard
import com.puraguia.app.util.dp
import com.puraguia.app.util.iniciales
import com.puraguia.app.util.setupHeader
import com.puraguia.app.util.toast

/**
 * Detalle de un tour. Se puede VER sin sesión; reservar, guardar en favoritos,
 * escribir reseña y mandar mensaje piden iniciar sesión (AuthGuard).
 *
 * TODO(equipo tours): cargar el tour real desde SQLite (hoy usa SampleData.tourPorId).
 */
class TourDetailActivity : AppCompatActivity() {

    private lateinit var tour: Tour
    private lateinit var imagenes: List<Int>
    private lateinit var imgGaleria: ImageView
    private lateinit var contenedorDots: LinearLayout
    private lateinit var btnFavorito: ImageButton
    private var indiceImagen = 0
    private var esFavorito = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tour_detail)

        val id = intent.getIntExtra(EXTRA_TOUR_ID, 1)
        tour = SampleData.tourPorId(id) ?: SampleData.tours.first()
        setupHeader(tour.titulo)

        imagenes = (listOf(tour.imagen, R.drawable.tour_volcan, R.drawable.tour_canopy, R.drawable.bg_tour_playa))
            .distinct().take(3)

        configurarGaleria()
        configurarFavorito()
        llenarDatos()
        configurarAcciones()
    }

    private fun configurarGaleria() {
        imgGaleria = findViewById(R.id.img_galeria)
        contenedorDots = findViewById(R.id.container_dots)
        findViewById<View>(R.id.btn_prev).setOnClickListener {
            indiceImagen = (indiceImagen - 1 + imagenes.size) % imagenes.size
            mostrarImagen()
        }
        findViewById<View>(R.id.btn_next).setOnClickListener {
            indiceImagen = (indiceImagen + 1) % imagenes.size
            mostrarImagen()
        }
        mostrarImagen()
    }

    private fun mostrarImagen() {
        imgGaleria.setImageResource(imagenes[indiceImagen])
        contenedorDots.removeAllViews()
        imagenes.indices.forEach { posicion ->
            val activo = posicion == indiceImagen
            val punto = View(this)
            val parametros = LinearLayout.LayoutParams(dp(if (activo) 24 else 8), dp(8))
            parametros.marginStart = dp(3)
            parametros.marginEnd = dp(3)
            punto.layoutParams = parametros
            punto.setBackgroundResource(if (activo) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            contenedorDots.addView(punto)
        }
    }

    private fun configurarFavorito() {
        btnFavorito = findViewById(R.id.header_action)
        btnFavorito.visibility = View.VISIBLE
        btnFavorito.setOnClickListener {
            AuthGuard.requireLogin(this, "Inicia sesión para guardar tus favoritos") {
                // TODO(equipo favoritos): guardar/quitar el favorito en SQLite (tabla favoritos).
                esFavorito = !esFavorito
                btnFavorito.setImageResource(if (esFavorito) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
                toast(if (esFavorito) "Agregado a favoritos" else "Quitado de favoritos")
            }
        }
    }

    private fun llenarDatos() {
        findViewById<TextView>(R.id.txt_guia_iniciales).text = iniciales(tour.guia)
        findViewById<TextView>(R.id.txt_guia_nombre).text = tour.guia
        findViewById<TextView>(R.id.txt_guia_rating).text = "${tour.rating} (${tour.resenas} reseñas)"
        findViewById<TextView>(R.id.txt_precio).text = "$${tour.precio}"
        findViewById<TextView>(R.id.txt_lugar).text = tour.lugar
        findViewById<TextView>(R.id.txt_idiomas).text = tour.idiomas.joinToString(", ")
        findViewById<TextView>(R.id.txt_duracion).text = tour.duracion
        findViewById<TextView>(R.id.txt_cuidados).text = tour.cuidados

        val tags = findViewById<LinearLayout>(R.id.container_idiomas_tags)
        tour.idiomas.forEach { idioma ->
            val tag = layoutInflater.inflate(R.layout.item_tag, tags, false) as TextView
            tag.text = idioma
            tags.addView(tag)
        }

        // Reseñas (de ejemplo)
        val resenas = SampleData.resenas
        findViewById<TextView>(R.id.txt_resenas_titulo).text = "Reseñas (${resenas.size})"
        val contenedor = findViewById<LinearLayout>(R.id.container_resenas)
        resenas.forEach { resena ->
            val fila = layoutInflater.inflate(R.layout.item_resena, contenedor, false)
            fila.findViewById<TextView>(R.id.txt_iniciales).text = iniciales(resena.autor)
            fila.findViewById<TextView>(R.id.txt_autor).text = resena.autor
            fila.findViewById<TextView>(R.id.txt_fecha).text = resena.fecha
            fila.findViewById<TextView>(R.id.txt_estrellas).text =
                "★".repeat(resena.estrellas) + "☆".repeat(5 - resena.estrellas)
            fila.findViewById<TextView>(R.id.txt_comentario).text = resena.comentario
            contenedor.addView(fila)
        }
    }

    private fun configurarAcciones() {
        findViewById<View>(R.id.row_guia).setOnClickListener {
            startActivity(GuideProfileActivity.createIntent(this, tour.guia))
        }

        findViewById<View>(R.id.btn_mensaje_guia).setOnClickListener {
            AuthGuard.requireLogin(this, "Inicia sesión para enviar mensajes") {
                startActivity(ChatActivity.createIntent(this, tour.guia, true))
            }
        }

        findViewById<View>(R.id.btn_escribir_resena).setOnClickListener {
            AuthGuard.requireLogin(this, "Inicia sesión para dejar una reseña") {
                // TODO(equipo reseñas): abrir el formulario de reseña y guardarla en SQLite.
                toast("Pantalla de reseña pendiente (equipo reseñas)")
            }
        }

        findViewById<View>(R.id.btn_reservar).setOnClickListener {
            AuthGuard.requireLogin(this, "Inicia sesión para reservar un tour") {
                startActivity(ReservaActivity.createIntent(this, tour.id))
            }
        }
    }

    companion object {
        private const val EXTRA_TOUR_ID = "tour_id"

        fun createIntent(context: Context, tourId: Int): Intent =
            Intent(context, TourDetailActivity::class.java).putExtra(EXTRA_TOUR_ID, tourId)
    }
}
