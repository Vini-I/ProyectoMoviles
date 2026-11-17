package com.puraguia.app.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.FavoritoDao
import com.puraguia.app.model.Tour
import com.puraguia.app.ui.common.TourCardFactory
import com.puraguia.app.ui.tour.TourDetailActivity
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.setupHeader
import com.puraguia.app.util.toast

/**
 * Pantalla "Favoritos": lista de tours guardados por el usuario en SQLite.
 */
class FavoritosActivity : AppCompatActivity() {

    private lateinit var favoritoDao: FavoritoDao
    private lateinit var session: SessionManager
    private lateinit var contenedor: LinearLayout
    private lateinit var txtVacio: TextView

    private var favoritos: List<Pair<Tour, Long>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favoritos)

        favoritoDao = FavoritoDao(this)
        session = SessionManager(this)
        setupHeader("Favoritos", "0 tours guardados")

        contenedor = findViewById(R.id.container_favoritos)
        txtVacio = findViewById(R.id.txt_vacio)
    }

    override fun onResume() {
        super.onResume()
        cargarFavoritos()
    }

    private fun cargarFavoritos() {
        favoritos = favoritoDao.obtenerToursFavoritos(session.getUserId())
        findViewById<TextView>(R.id.header_subtitle).text = textoCantidad(favoritos.size)
        contenedor.removeAllViews()
        txtVacio.visibility = if (favoritos.isEmpty()) View.VISIBLE else View.GONE

        favoritos.forEach { (tour, fechaGuardado) ->
            val tarjeta = layoutInflater.inflate(R.layout.item_favorito, contenedor, false)
            TourCardFactory.cargarImagen(tarjeta.findViewById<ImageView>(R.id.img_tour), tour)
            tarjeta.findViewById<TextView>(R.id.txt_titulo).text = tour.titulo
            tarjeta.findViewById<TextView>(R.id.txt_rating).text = tour.rating.toString()
            tarjeta.findViewById<TextView>(R.id.txt_lugar).text = tour.lugar
            tarjeta.findViewById<TextView>(R.id.txt_precio).text =
                "$${tour.precioOriginal ?: tour.precio}"
            tarjeta.findViewById<TextView>(R.id.txt_guardado_hace).text = formatearTiempo(fechaGuardado)

            val abrirDetalle = View.OnClickListener {
                startActivity(TourDetailActivity.createIntent(this, tour.id))
            }
            tarjeta.setOnClickListener(abrirDetalle)
            tarjeta.findViewById<View>(R.id.btn_ver).setOnClickListener(abrirDetalle)

            tarjeta.findViewById<View>(R.id.btn_quitar_favorito).setOnClickListener {
                if (favoritoDao.eliminar(session.getUserId(), tour.id)) {
                    toast("Quitado de favoritos")
                    cargarFavoritos()
                }
            }

            contenedor.addView(tarjeta)
        }
    }

    private fun formatearTiempo(timestamp: Long): String {
        val dias = ((System.currentTimeMillis() - timestamp) / 86_400_000L).toInt()
        return when {
            dias <= 0 -> "Hoy"
            dias == 1 -> "Ayer"
            dias < 7 -> "Hace $dias días"
            else -> "Hace ${dias / 7} sem"
        }
    }

    private fun textoCantidad(cantidad: Int): String =
        if (cantidad == 1) "1 tour guardado" else "$cantidad tours guardados"
}
