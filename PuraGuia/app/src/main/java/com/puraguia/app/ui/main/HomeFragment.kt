package com.puraguia.app.ui.main

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.puraguia.app.R
import com.puraguia.app.data.TourDAO
import com.puraguia.app.model.Tour
import com.puraguia.app.ui.auth.LoginActivity
import com.puraguia.app.ui.common.TourCardFactory
import com.puraguia.app.ui.tour.TourDetailActivity
import com.puraguia.app.util.SessionManager

/** Pestaña "Inicio": bienvenida, ofertas y tours populares cargados desde SQLite. */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var tourDao: TourDAO
    private lateinit var btnIngresar: TextView
    private lateinit var txtBienvenida: TextView
    private lateinit var contenedorOfertas: LinearLayout
    private lateinit var contenedorPopulares: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tourDao = TourDAO(requireContext())
        btnIngresar = view.findViewById(R.id.btn_ingresar)
        txtBienvenida = view.findViewById(R.id.txt_bienvenida)
        contenedorOfertas = view.findViewById(R.id.container_ofertas)
        contenedorPopulares = view.findViewById(R.id.container_populares)

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

        cargarToursDesdeSqlite()
    }

    private fun cargarToursDesdeSqlite() {
        val ofertas = tourDao.obtenerOfertas()
        val populares = tourDao.obtenerPopulares(8)
        llenar(contenedorOfertas, if (ofertas.isNotEmpty()) ofertas else populares.take(3))
        llenar(contenedorPopulares, populares)
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
