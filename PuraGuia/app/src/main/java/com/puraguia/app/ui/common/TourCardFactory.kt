package com.puraguia.app.ui.common

import android.graphics.Paint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.puraguia.app.R
import com.puraguia.app.model.Tour
import java.io.File

/** Crea las tarjetas de tour que se reutilizan en Inicio, Buscar, Favoritos y el perfil del guía. */
object TourCardFactory {

    fun cargarImagen(img: ImageView, tour: Tour) {
        val ruta = tour.imagenPath
        if (!ruta.isNullOrBlank()) {
            val archivo = File(ruta)
            if (archivo.exists()) {
                img.setImageURI(Uri.fromFile(archivo))
                return
            }
        }
        img.setImageResource(tour.imagen)
    }

    /** Tarjeta grande (carruseles de la pantalla de inicio). */
    fun crearTarjeta(
        inflater: LayoutInflater,
        padre: ViewGroup,
        tour: Tour,
        alTocar: (Tour) -> Unit
    ): View {
        val vista = inflater.inflate(R.layout.item_tour_card, padre, false)
        cargarImagen(vista.findViewById(R.id.img_tour), tour)
        vista.findViewById<TextView>(R.id.txt_rating).text = tour.rating.toString()
        vista.findViewById<TextView>(R.id.txt_titulo).text = tour.titulo
        vista.findViewById<TextView>(R.id.txt_lugar).text = tour.lugar
        vista.findViewById<TextView>(R.id.txt_precio).text = "$${tour.precio}"

        val descuento = vista.findViewById<TextView>(R.id.txt_descuento)
        val original = vista.findViewById<TextView>(R.id.txt_precio_original)
        if (tour.descuento != null && tour.precioOriginal != null) {
            descuento.text = "-${tour.descuento}%"
            descuento.visibility = View.VISIBLE
            original.text = "$${tour.precioOriginal}"
            original.paintFlags = original.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            original.visibility = View.VISIBLE
        }
        vista.setOnClickListener { alTocar(tour) }
        return vista
    }

    /** Fila horizontal compacta (resultados de búsqueda). */
    fun crearFila(
        inflater: LayoutInflater,
        padre: ViewGroup,
        tour: Tour,
        alTocar: (Tour) -> Unit
    ): View {
        val vista = inflater.inflate(R.layout.item_tour_row, padre, false)
        cargarImagen(vista.findViewById(R.id.img_tour), tour)
        vista.findViewById<TextView>(R.id.txt_rating).text = tour.rating.toString()
        vista.findViewById<TextView>(R.id.txt_titulo).text = tour.titulo
        vista.findViewById<TextView>(R.id.txt_lugar).text = tour.lugar
        vista.findViewById<TextView>(R.id.txt_resenas).text = "${tour.resenas} reseñas"
        vista.findViewById<TextView>(R.id.txt_precio).text = "$${tour.precio}"
        vista.setOnClickListener { alTocar(tour) }
        return vista
    }
}
