package com.puraguia.app.ui.main

import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.puraguia.app.R
import com.puraguia.app.data.TourDAO
import com.puraguia.app.model.Tour
import com.puraguia.app.ui.common.TourCardFactory
import com.puraguia.app.ui.tour.TourDetailActivity
import com.puraguia.app.util.ChipGrid
import com.puraguia.app.util.dp

/**
 * Pestaña "Buscar": consulta tours directamente en SQLite con búsqueda por texto,
 * provincia, categorías, duración y ordenamiento.
 */
class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var tourDao: TourDAO
    private lateinit var chipsCategorias: ChipGrid
    private lateinit var chipsDuracion: ChipGrid
    private lateinit var editBuscar: EditText
    private lateinit var spinnerProvincia: Spinner
    private lateinit var spinnerOrden: Spinner
    private lateinit var layoutFiltrosActivos: View
    private lateinit var containerFiltrosActivos: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tourDao = TourDAO(requireContext())
        editBuscar = view.findViewById(R.id.edit_buscar)
        spinnerProvincia = view.findViewById(R.id.spinner_provincia)
        spinnerOrden = view.findViewById(R.id.spinner_orden)
        layoutFiltrosActivos = view.findViewById(R.id.layout_filtros_activos)
        containerFiltrosActivos = view.findViewById(R.id.container_filtros_activos)

        val btnLimpiar = view.findViewById<TextView>(R.id.btn_limpiar_filtros)
        btnLimpiar.paintFlags = btnLimpiar.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        btnLimpiar.setOnClickListener {
            editBuscar.text.clear()
            spinnerProvincia.setSelection(0)
            spinnerOrden.setSelection(0)
            chipsCategorias.limpiar()
            chipsDuracion.limpiar()
            aplicarFiltros()
        }

        chipsCategorias = ChipGrid(
            requireContext(),
            view.findViewById<GridLayout>(R.id.grid_categorias),
            listOf("Aventura", "Naturaleza", "Avistamiento", "Familiar", "Cultura", "Playa"),
            seleccionMultiple = true,
            columnas = 2,
            alturaDp = 46,
            alCambiar = { aplicarFiltros() }
        )
        chipsDuracion = ChipGrid(
            requireContext(),
            view.findViewById<GridLayout>(R.id.grid_duracion),
            listOf("2-3h", "4-5h", "6-7h", "8-10h", "+1 día"),
            seleccionMultiple = false,
            columnas = 3,
            alturaDp = 46,
            alCambiar = { aplicarFiltros() }
        )

        editBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                aplicarFiltros()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        editBuscar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                aplicarFiltros()
                true
            } else {
                false
            }
        }

        view.findViewById<View>(R.id.btn_buscar).setOnClickListener { aplicarFiltros() }

        val listenerSpinner = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                aplicarFiltros()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerProvincia.onItemSelectedListener = listenerSpinner
        spinnerOrden.onItemSelectedListener = listenerSpinner
    }

    override fun onResume() {
        super.onResume()
        aplicarFiltros()
    }

    private fun aplicarFiltros() {
        if (!isAdded) return
        actualizarFiltrosActivos()
        val provincia = spinnerProvincia.selectedItem?.toString()
        val resultados = tourDao.buscar(
            query = editBuscar.text.toString(),
            provincia = provincia,
            categorias = chipsCategorias.seleccionados,
            duracionFiltro = chipsDuracion.seleccionados.firstOrNull(),
            orden = spinnerOrden.selectedItemPosition
        )
        mostrarResultados(resultados)
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
                aplicarFiltros()
            }
            containerFiltrosActivos.addView(pill, params)
        }
    }

    private fun mostrarResultados(tours: List<Tour>) {
        val vista = view ?: return
        vista.findViewById<TextView>(R.id.txt_resultados).text =
            if (tours.size == 1) "1 resultado" else "${tours.size} resultados"
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
