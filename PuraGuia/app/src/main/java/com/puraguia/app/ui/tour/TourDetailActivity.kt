package com.puraguia.app.ui.tour

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.ActividadDao
import com.puraguia.app.data.FavoritoDao
import com.puraguia.app.data.ResenaDao
import com.puraguia.app.data.TourDAO
import com.puraguia.app.model.TipoActividad
import com.puraguia.app.model.Tour
import com.puraguia.app.ui.chat.ChatActivity
import com.puraguia.app.ui.profile.AgregarTourActivity
import com.puraguia.app.util.AuthGuard
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.dp
import com.puraguia.app.util.iniciales
import com.puraguia.app.util.setupHeader
import com.puraguia.app.util.toast
import java.io.File

/**
 * Detalle de un tour cargado desde SQLite.
 * Permite consultar datos, favoritos, reseñas, mensajería, reservas,
 * y (si el usuario es el guía propietario) editar o eliminar el tour.
 */
class TourDetailActivity : AppCompatActivity() {

    private lateinit var tourDao: TourDAO
    private lateinit var favoritoDao: FavoritoDao
    private lateinit var resenaDao: ResenaDao
    private lateinit var actividadDao: ActividadDao
    private lateinit var session: SessionManager

    private var tourId: Int = -1
    private lateinit var tour: Tour
    private lateinit var imagenes: List<Int>
    private lateinit var imgGaleria: ImageView
    private lateinit var contenedorDots: LinearLayout
    private lateinit var btnFavorito: ImageButton
    private var indiceImagen = 0
    private var esFavorito = false

    private val editarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            cargarTourDesdeSqlite()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tour_detail)

        tourDao = TourDAO(this)
        favoritoDao = FavoritoDao(this)
        resenaDao = ResenaDao(this)
        actividadDao = ActividadDao(this)
        session = SessionManager(this)

        tourId = intent.getIntExtra(EXTRA_TOUR_ID, -1)
        imgGaleria = findViewById(R.id.img_galeria)
        contenedorDots = findViewById(R.id.container_dots)
        btnFavorito = findViewById(R.id.header_action)
        btnFavorito.visibility = View.VISIBLE

        if (!cargarTourDesdeSqlite()) return
        configurarGaleria()
        configurarFavorito()
        configurarAcciones()
    }

    override fun onResume() {
        super.onResume()
        if (tourId > 0) {
            cargarTourDesdeSqlite()
        }
    }

    private fun cargarTourDesdeSqlite(): Boolean {
        val encontrado = tourDao.obtenerPorId(tourId) ?: tourDao.obtenerTodos().firstOrNull()
        if (encontrado == null) {
            toast("El tour ya no está disponible")
            finish()
            return false
        }
        tour = encontrado
        tourId = tour.id
        setupHeader(tour.titulo)

        imagenes = listOf(tour.imagen, R.drawable.tour_volcan, R.drawable.tour_canopy, R.drawable.bg_tour_playa)
            .distinct().take(3)

        esFavorito = session.isLoggedIn() && favoritoDao.esFavorito(session.getUserId(), tour.id)
        btnFavorito.setImageResource(if (esFavorito) R.drawable.ic_favorite else R.drawable.ic_favorite_border)

        mostrarImagen()
        llenarDatos()
        actualizarAccionesGuia()
        return true
    }

    private fun configurarGaleria() {
        findViewById<View>(R.id.btn_prev).setOnClickListener {
            indiceImagen = (indiceImagen - 1 + imagenes.size) % imagenes.size
            mostrarImagen()
        }
        findViewById<View>(R.id.btn_next).setOnClickListener {
            indiceImagen = (indiceImagen + 1) % imagenes.size
            mostrarImagen()
        }
    }

    private fun mostrarImagen() {
        val ruta = tour.imagenPath
        if (indiceImagen == 0 && !ruta.isNullOrBlank() && File(ruta).exists()) {
            imgGaleria.setImageURI(Uri.fromFile(File(ruta)))
        } else {
            imgGaleria.setImageResource(imagenes[indiceImagen])
        }
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
        btnFavorito.setOnClickListener {
            AuthGuard.requireLogin(this, "Inicia sesión para guardar tus favoritos") {
                esFavorito = favoritoDao.alternar(session.getUserId(), tour.id)
                btnFavorito.setImageResource(if (esFavorito) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
                if (esFavorito) {
                    actividadDao.registrar(
                        session.getUserId(),
                        TipoActividad.FAVORITO,
                        "Tour agregado a favoritos",
                        tour.titulo
                    )
                }
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
        tags.removeAllViews()
        tour.idiomas.forEach { idioma ->
            val tag = layoutInflater.inflate(R.layout.item_tag, tags, false) as TextView
            tag.text = idioma
            tags.addView(tag)
        }

        val resenas = resenaDao.obtenerPorTour(tour.id)
        findViewById<TextView>(R.id.txt_resenas_titulo).text = "Reseñas (${resenas.size})"
        val contenedor = findViewById<LinearLayout>(R.id.container_resenas)
        contenedor.removeAllViews()
        if (resenas.isEmpty()) {
            val vacio = TextView(this).apply {
                text = "Aún no hay reseñas. ¡Sé el primero en opinar!"
                textSize = 14f
                setPadding(0, dp(8), 0, dp(8))
            }
            contenedor.addView(vacio)
        } else {
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
    }

    private fun actualizarAccionesGuia() {
        val layoutGuia = findViewById<View>(R.id.layout_acciones_guia)
        val esPropietario = session.isLoggedIn() && session.esGuia() && tour.guiaId == session.getUserId()
        layoutGuia.visibility = if (esPropietario) View.VISIBLE else View.GONE
    }

    private fun configurarAcciones() {
        findViewById<View>(R.id.row_guia).setOnClickListener {
            startActivity(GuideProfileActivity.createIntent(this, tour.guia, tour.guiaId))
        }

        findViewById<View>(R.id.btn_mensaje_guia).setOnClickListener {
            AuthGuard.requireLogin(this, "Inicia sesión para enviar mensajes") {
                startActivity(ChatActivity.createIntent(this, tour.guia, true, tour.guiaId))
            }
        }

        findViewById<View>(R.id.btn_editar_tour).setOnClickListener {
            editarLauncher.launch(AgregarTourActivity.createEditIntent(this, tour.id))
        }

        findViewById<View>(R.id.btn_eliminar_tour).setOnClickListener {
            confirmarEliminarTour()
        }

        findViewById<View>(R.id.btn_escribir_resena).setOnClickListener {
            AuthGuard.requireLogin(this, "Inicia sesión para dejar una reseña") {
                mostrarDialogoResena()
            }
        }

        findViewById<View>(R.id.btn_reservar).setOnClickListener {
            AuthGuard.requireLogin(this, "Inicia sesión para reservar un tour") {
                startActivity(ReservaActivity.createIntent(this, tour.id))
            }
        }
    }

    private fun confirmarEliminarTour() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tour")
            .setMessage("¿Seguro que deseas eliminar \"${tour.titulo}\"? Esta acción no se puede deshacer.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                if (tourDao.eliminar(tour.id)) {
                    toast("Tour eliminado correctamente")
                    finish()
                } else {
                    toast("No se pudo eliminar el tour")
                }
            }
            .show()
    }

    private fun mostrarDialogoResena() {
        val ratingBar = RatingBar(this, null, android.R.attr.ratingBarStyle).apply {
            numStars = 5
            stepSize = 1f
            rating = 5f
        }
        val editComentario = EditText(this).apply {
            hint = "Cuéntanos cómo estuvo el tour..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 3
        }
        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
            addView(ratingBar)
            addView(editComentario)
        }

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Escribir reseña")
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Publicar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val comentario = editComentario.text.toString().trim()
                if (comentario.isEmpty()) {
                    editComentario.error = "Escribe un comentario"
                    return@setOnClickListener
                }
                val estrellas = ratingBar.rating.toInt().coerceIn(1, 5)
                val id = resenaDao.insertar(
                    tourId = tour.id,
                    usuarioId = session.getUserId(),
                    autorNombre = session.getNombre(),
                    estrellas = estrellas,
                    comentario = comentario
                )
                if (id > 0L) {
                    actividadDao.registrar(
                        session.getUserId(),
                        TipoActividad.RESENA,
                        "Reseña publicada",
                        "Valoraste \"${tour.titulo}\" con $estrellas estrellas"
                    )
                    toast("¡Reseña publicada!")
                    dialogo.dismiss()
                    cargarTourDesdeSqlite()
                } else {
                    toast("No se pudo guardar la reseña")
                }
            }
        }
        dialogo.show()
    }

    companion object {
        private const val EXTRA_TOUR_ID = "tour_id"

        fun createIntent(context: Context, tourId: Int): Intent =
            Intent(context, TourDetailActivity::class.java).putExtra(EXTRA_TOUR_ID, tourId)
    }
}
