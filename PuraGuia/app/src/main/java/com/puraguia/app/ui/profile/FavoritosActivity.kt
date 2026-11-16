package com.puraguia.app.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.SampleData
import com.puraguia.app.model.Tour
import com.puraguia.app.ui.tour.TourDetailActivity
import com.puraguia.app.util.setupHeader
import com.puraguia.app.util.toast

/**
 * Pantalla "Favoritos": lista de tours guardados por el usuario.
 *
 * TODO(equipo favoritos): cargar y quitar favoritos directamente desde SQLite (tabla `favoritos`
 * filtrada por `SessionManager(this).getUserId()`). Hoy muestra tours de ejemplo.
 */
class FavoritosActivity : AppCompatActivity() {

    private lateinit var contenedor: LinearLayout
    private lateinit var txtVacio: TextView

    private val favoritos: MutableList<Tour> =
        SampleData.tours.filter { it.id in listOf(1, 2, 4) }.toMutableList()

    private val tiemposEjemplo = mapOf(
        1 to "Hace 2 días",
        2 to "Hace 1 semana",
        4 to "Hace 2 semanas"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favoritos)
        setupHeader("Favoritos", textoCantidad(favoritos.size))

        contenedor = findViewById(R.id.container_favoritos)
        txtVacio = findViewById(R.id.txt_vacio)

        mostrarFavoritos()
    }

    private fun mostrarFavoritos() {
        findViewById<TextView>(R.id.header_subtitle).text = textoCantidad(favoritos.size)
        contenedor.removeAllViews()
        txtVacio.visibility = if (favoritos.isEmpty()) View.VISIBLE else View.GONE

        favoritos.toList().forEach { tour ->
            val tarjeta = layoutInflater.inflate(R.layout.item_favorito, contenedor, false)
            tarjeta.findViewById<ImageView>(R.id.img_tour).setImageResource(tour.imagen)
            tarjeta.findViewById<TextView>(R.id.txt_titulo).text = tour.titulo
            tarjeta.findViewById<TextView>(R.id.txt_rating).text = tour.rating.toString()
            tarjeta.findViewById<TextView>(R.id.txt_lugar).text = tour.lugar
            tarjeta.findViewById<TextView>(R.id.txt_precio).text =
                "$${tour.precioOriginal ?: tour.precio}"
            tarjeta.findViewById<TextView>(R.id.txt_guardado_hace).text =
                tiemposEjemplo[tour.id] ?: "Reciente"

            val abrirDetalle = View.OnClickListener {
                startActivity(TourDetailActivity.createIntent(this, tour.id))
            }
            tarjeta.setOnClickListener(abrirDetalle)
            tarjeta.findViewById<View>(R.id.btn_ver).setOnClickListener(abrirDetalle)

            tarjeta.findViewById<View>(R.id.btn_quitar_favorito).setOnClickListener {
                // TODO(equipo favoritos): eliminar de SQLite
                favoritos.remove(tour)
                toast("Quitado de favoritos")
                mostrarFavoritos()
            }

            contenedor.addView(tarjeta)
        }
    }

    private fun textoCantidad(cantidad: Int): String =
        if (cantidad == 1) "1 tour guardado" else "$cantidad tours guardados"
}
