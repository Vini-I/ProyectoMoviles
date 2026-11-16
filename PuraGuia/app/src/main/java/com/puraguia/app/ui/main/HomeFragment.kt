package com.puraguia.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.puraguia.app.R
import com.puraguia.app.data.SampleData
import com.puraguia.app.model.Tour
import com.puraguia.app.ui.auth.LoginActivity
import com.puraguia.app.ui.common.TourCardFactory
import com.puraguia.app.ui.tour.TourDetailActivity
import com.puraguia.app.util.SessionManager

/** Pestaña "Inicio": bienvenida, ofertas y tours populares. */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var btnIngresar: TextView
    private lateinit var txtBienvenida: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnIngresar = view.findViewById(R.id.btn_ingresar)
        txtBienvenida = view.findViewById(R.id.txt_bienvenida)

        // TODO(equipo tours): reemplazar SampleData por consultas a SQLite (ofertas y más reservados)
        val contenedorOfertas = view.findViewById<LinearLayout>(R.id.container_ofertas)
        val contenedorPopulares = view.findViewById<LinearLayout>(R.id.container_populares)
        llenar(contenedorOfertas, SampleData.tours.filter { it.descuento != null })
        llenar(contenedorPopulares, SampleData.tours.sortedByDescending { it.resenas }.take(5))

        view.findViewById<View>(R.id.btn_ver_todo_ofertas).setOnClickListener { irABuscar() }
        view.findViewById<View>(R.id.btn_ver_todo_populares).setOnClickListener { irABuscar() }
    }

    override fun onResume() {
        super.onResume()
        val session = SessionManager(requireContext())
        if (session.isLoggedIn()) {
            val primerNombre = session.getNombre().substringBefore(' ')
            btnIngresar.visibility = View.GONE
            txtBienvenida.text = "¡Pura vida, $primerNombre!"
        } else {
            btnIngresar.visibility = View.VISIBLE
            btnIngresar.text = "Ingresar"
            txtBienvenida.text = "¡Pura vida! Bienvenido"
            btnIngresar.setOnClickListener {
                startActivity(LoginActivity.createIntent(requireContext()))
            }
        }
    }

    private fun llenar(contenedor: LinearLayout, tours: List<Tour>) {
        contenedor.removeAllViews()
        tours.forEach { tour ->
            contenedor.addView(
                TourCardFactory.crearTarjeta(layoutInflater, contenedor as ViewGroup, tour) { abrirTour(it) }
            )
        }
    }

    private fun abrirTour(tour: Tour) {
        startActivity(TourDetailActivity.createIntent(requireContext(), tour.id))
    }

    private fun irABuscar() {
        (activity as? MainActivity)?.irABuscar()
    }
}
