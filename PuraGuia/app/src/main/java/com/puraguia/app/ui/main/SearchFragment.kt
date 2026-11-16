package com.puraguia.app.ui.main

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.puraguia.app.R
import com.puraguia.app.data.SampleData
import com.puraguia.app.model.Tour
import com.puraguia.app.ui.common.TourCardFactory
import com.puraguia.app.ui.tour.TourDetailActivity
import com.puraguia.app.util.ChipGrid
import com.puraguia.app.util.dp

/**
 * Pestaña "Buscar": cuadro de búsqueda, provincia, categorías, duración y resultados.
 *
 * TODO(equipo búsqueda): aquí falta la LÓGICA de filtrado. Los controles ya existen:
 *  - edit_buscar (texto), spinner_provincia, chipsCategorias, chipsDuracion, spinner_orden
 *  - mostrarResultados(lista) pinta los resultados y actualiza el contador.
 *  Solo hay que consultar SQLite con los filtros elegidos y llamar a mostrarResultados().
 */
class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var chipsCategorias: ChipGrid
    private lateinit var chipsDuracion: ChipGrid
    private lateinit var editBuscar: EditText
    private lateinit var layoutFiltrosActivos: View
    private lateinit var containerFiltrosActivos: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editBuscar = view.findViewById(R.id.edit_buscar)
        layoutFiltrosActivos = view.findViewById(R.id.layout_filtros_activos)
        containerFiltrosActivos = view.findViewById(R.id.container_filtros_activos)

        val btnLimpiar = view.findViewById<TextView>(R.id.btn_limpiar_filtros)
        btnLimpiar.paintFlags = btnLimpiar.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        btnLimpiar.setOnClickListener {
            chipsCategorias.limpiar()
            chipsDuracion.limpiar()
            actualizarFiltrosActivos()
        }

        chipsCategorias = ChipGrid(
            requireContext(),
            view.findViewById<GridLayout>(R.id.grid_categorias),
            listOf("Cerca", "Avistamiento", "Familiar", "Aventura"),
            seleccionMultiple = true,
            columnas = 2,
            alturaDp = 46,
            alCambiar = { actualizarFiltrosActivos() }
        )
        chipsDuracion = ChipGrid(
            requireContext(),
            view.findViewById<GridLayout>(R.id.grid_duracion),
            listOf("2-3h", "4-5h", "6-7h", "8-10h", "+1 día"),
            seleccionMultiple = false,
            columnas = 3,
            alturaDp = 46,
            alCambiar = { actualizarFiltrosActivos() }
        )

        actualizarFiltrosActivos()
        // Por ahora se muestran todos los tours de ejemplo
        mostrarResultados(SampleData.tours)
    }

    private fun actualizarFiltrosActivos() {
        val activos = chipsCategorias.seleccionados + chipsDuracion.seleccionados
        if (activos.isEmpty()) {
            layoutFiltrosActivos.visibility = View.GONE
            containerFiltrosActivos.removeAllViews()
            return
        }
        layoutFiltrosActivos.visibility = View.VISIBLE
        containerFiltrosActivos.removeAllViews()

        val ctx = requireContext()
        activos.forEach { texto ->
            val pill = TextView(ctx)
            pill.text = "$texto  ✕"
            pill.textSize = 13f
            pill.setTextColor(ContextCompat.getColor(ctx, R.color.pg_blue))
            pill.setBackgroundResource(R.drawable.bg_filter_active_pill)
            pill.setPadding(ctx.dp(12), ctx.dp(5), ctx.dp(12), ctx.dp(5))
            pill.isClickable = true
            pill.isFocusable = true
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = ctx.dp(6)
            pill.setOnClickListener {
                chipsCategorias.deseleccionar(texto)
                chipsDuracion.deseleccionar(texto)
                actualizarFiltrosActivos()
            }
            containerFiltrosActivos.addView(pill, params)
        }
    }

    private fun mostrarResultados(tours: List<Tour>) {
        val vista = view ?: return
        vista.findViewById<TextView>(R.id.txt_resultados).text = "${tours.size} resultados"
        val contenedor = vista.findViewById<LinearLayout>(R.id.container_resultados)
        contenedor.removeAllViews()
        tours.forEach { tour ->
            contenedor.addView(
                TourCardFactory.crearFila(layoutInflater, contenedor, tour) {
                    startActivity(TourDetailActivity.createIntent(requireContext(), it.id))
                }
            )
        }
    }
}
