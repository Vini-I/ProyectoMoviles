package com.puraguia.app.ui.main

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.puraguia.app.R
import com.puraguia.app.data.SampleData
import com.puraguia.app.ui.profile.ActividadRecienteActivity
import com.puraguia.app.ui.profile.ConfiguracionActivity
import com.puraguia.app.ui.profile.FavoritosActivity
import com.puraguia.app.ui.profile.MetodosPagoActivity
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.iniciales
import com.puraguia.app.util.toast

/**
 * Pestaña "Perfil": datos de la sesión, accesos (Pago, Favoritos, Configuración, Recientes)
 * e historial de reservas. Solo se puede entrar con sesión iniciada (MainActivity lo valida).
 *
 * TODO(equipo perfil): las estadísticas y el historial son de ejemplo; hay que leerlos de SQLite.
 */
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        view.findViewById<TextView>(R.id.txt_nombre).text = session.getNombre()
        view.findViewById<TextView>(R.id.txt_email).text = session.getEmail()
        view.findViewById<TextView>(R.id.txt_avatar).text = iniciales(session.getNombre()).take(1)
        view.findViewById<TextView>(R.id.txt_rol).text =
            if (session.esGuia()) "Guía" else "Turista"

        // Estadísticas de ejemplo
        view.findViewById<TextView>(R.id.txt_num_reservas).text = SampleData.historialReservas.size.toString()
        view.findViewById<TextView>(R.id.txt_num_resenas).text = "3"

        view.findViewById<View>(R.id.row_pago).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), MetodosPagoActivity::class.java))
        }
        view.findViewById<View>(R.id.row_favoritos).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), FavoritosActivity::class.java))
        }
        view.findViewById<View>(R.id.row_configuracion).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), ConfiguracionActivity::class.java))
        }
        view.findViewById<View>(R.id.row_recientes).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), ActividadRecienteActivity::class.java))
        }

        // Historial de reservas (ejemplo)
        val contenedor = view.findViewById<LinearLayout>(R.id.container_historial)
        contenedor.removeAllViews()
        SampleData.historialReservas.forEach { reserva ->
            val fila = layoutInflater.inflate(R.layout.item_historial, contenedor, false)
            fila.findViewById<TextView>(R.id.txt_titulo).text = reserva.tour
            fila.findViewById<TextView>(R.id.txt_estado).text = reserva.estado
            fila.findViewById<TextView>(R.id.txt_fecha).text = reserva.fecha
            fila.findViewById<TextView>(R.id.txt_total).text = "$${reserva.total}"
            contenedor.addView(fila)
        }

        view.findViewById<View>(R.id.btn_cerrar_sesion).setOnClickListener { confirmarCerrarSesion(session) }
    }

    private fun confirmarCerrarSesion(session: SessionManager) {
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
