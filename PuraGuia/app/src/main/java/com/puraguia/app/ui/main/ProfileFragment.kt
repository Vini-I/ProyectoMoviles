package com.puraguia.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.puraguia.app.R
import com.puraguia.app.data.ResenaDao
import com.puraguia.app.data.ReservaDao
import com.puraguia.app.ui.profile.ActividadRecienteActivity
import com.puraguia.app.ui.profile.AgregarTourActivity
import com.puraguia.app.ui.profile.ConfiguracionActivity
import com.puraguia.app.ui.profile.FavoritosActivity
import com.puraguia.app.ui.profile.MetodosPagoActivity
import com.puraguia.app.ui.tour.TourDetailActivity
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.dp
import com.puraguia.app.util.iniciales
import com.puraguia.app.util.toast

/**
 * Pestaña Perfil: datos de sesión, accesos, agregar tour (para guías)
 * e historial real de reservas y estadísticas desde SQLite.
 */
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var session: SessionManager
    private lateinit var reservaDao: ReservaDao
    private lateinit var resenaDao: ResenaDao

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        reservaDao = ReservaDao(requireContext())
        resenaDao = ResenaDao(requireContext())

        val rowAgregarTour = view.findViewById<View>(R.id.row_agregar_tour)
        rowAgregarTour.visibility = if (session.esGuia()) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.divider_agregar_tour).visibility =
            if (session.esGuia()) View.VISIBLE else View.GONE

        rowAgregarTour.setOnClickListener {
            startActivity(Intent(requireContext(), AgregarTourActivity::class.java))
        }

        view.findViewById<View>(R.id.row_pago).setOnClickListener {
            startActivity(Intent(requireContext(), MetodosPagoActivity::class.java))
        }

        view.findViewById<View>(R.id.row_favoritos).setOnClickListener {
            startActivity(Intent(requireContext(), FavoritosActivity::class.java))
        }

        view.findViewById<View>(R.id.row_configuracion).setOnClickListener {
            startActivity(Intent(requireContext(), ConfiguracionActivity::class.java))
        }

        view.findViewById<View>(R.id.row_recientes).setOnClickListener {
            startActivity(Intent(requireContext(), ActividadRecienteActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_cerrar_sesion).setOnClickListener {
            confirmarCerrarSesion()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isAdded || !session.isLoggedIn()) return
        val v = view ?: return
        val userId = session.getUserId()

        v.findViewById<TextView>(R.id.txt_nombre).text = session.getNombre()
        v.findViewById<TextView>(R.id.txt_email).text = session.getEmail()
        v.findViewById<TextView>(R.id.txt_avatar).text = iniciales(session.getNombre()).take(1)
        v.findViewById<TextView>(R.id.txt_rol).text = if (session.esGuia()) "Guía" else "Turista"

        v.findViewById<TextView>(R.id.txt_num_reservas).text =
            reservaDao.contarPorUsuario(userId).toString()
        v.findViewById<TextView>(R.id.txt_num_resenas).text =
            resenaDao.contarPorUsuario(userId).toString()

        val contenedor = v.findViewById<LinearLayout>(R.id.container_historial)
        contenedor.removeAllViews()
        val reservas = reservaDao.obtenerPorUsuario(userId)

        if (reservas.isEmpty()) {
            val vacio = TextView(requireContext()).apply {
                text = "Aún no tienes reservas realizadas."
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_text_muted))
                setPadding(0, requireContext().dp(8), 0, requireContext().dp(4))
            }
            contenedor.addView(vacio)
        } else {
            reservas.forEach { reserva ->
                val fila = layoutInflater.inflate(R.layout.item_historial, contenedor, false)
                fila.findViewById<TextView>(R.id.txt_titulo).text = reserva.tour
                fila.findViewById<TextView>(R.id.txt_estado).text = reserva.estado
                val fechaBase = if (reserva.hora.isNotBlank()) "${reserva.fecha} · ${reserva.hora}" else reserva.fecha
                fila.findViewById<TextView>(R.id.txt_fecha).text =
                    if (reserva.metodoPago.isNotBlank()) "$fechaBase\n${reserva.metodoPago}" else fechaBase
                fila.findViewById<TextView>(R.id.txt_total).text = "$${reserva.total}"
                fila.setOnClickListener {
                    startActivity(TourDetailActivity.createIntent(requireContext(), reserva.tourId))
                }
                contenedor.addView(fila)
            }
        }
    }

    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que deseas cerrar tu sesión?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Cerrar sesión") { _, _ ->
                session.cerrarSesion()
                requireContext().toast("Sesión cerrada")
                (activity as? MainActivity)?.irAInicio()
            }
            .show()
    }
}
