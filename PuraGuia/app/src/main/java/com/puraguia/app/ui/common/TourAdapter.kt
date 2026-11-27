package com.puraguia.app.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.puraguia.app.R
import com.puraguia.app.model.Tour

class TourAdapter(
    private val alTocar: (Tour) -> Unit
) : RecyclerView.Adapter<TourAdapter.TourViewHolder>() {

    private var tours: List<Tour> = emptyList()

    class TourViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {

        val imagen: ImageView = vista.findViewById(R.id.img_tour)
        val titulo: TextView = vista.findViewById(R.id.txt_titulo)
        val lugar: TextView = vista.findViewById(R.id.txt_lugar)
        val rating: TextView = vista.findViewById(R.id.txt_rating)
        val resenas: TextView = vista.findViewById(R.id.txt_resenas)
        val precio: TextView = vista.findViewById(R.id.txt_precio)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TourViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tour_row, parent, false)

        return TourViewHolder(vista)
    }

    override fun onBindViewHolder(
        holder: TourViewHolder,
        position: Int
    ) {
        val tour = tours[position]

        TourCardFactory.cargarImagen(holder.imagen, tour)

        holder.titulo.text = tour.titulo
        holder.lugar.text = tour.lugar
        holder.rating.text = tour.rating.toString()
        holder.resenas.text = "${tour.resenas} reseñas"
        holder.precio.text = "$${tour.precio}"

        holder.itemView.setOnClickListener {
            alTocar(tour)
        }
    }

    override fun getItemCount(): Int {
        return tours.size
    }

    fun actualizarTours(nuevosTours: List<Tour>) {
        tours = nuevosTours.toList()
        notifyDataSetChanged()
    }
}