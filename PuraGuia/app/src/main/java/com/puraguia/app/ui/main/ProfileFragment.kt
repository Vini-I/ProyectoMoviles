
package com.puraguia.app.ui.main

import android.content.Intent
import android.os.Bundle

import android.view.View

import android.widget.LinearLayout

import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.puraguia.app.R
import com.puraguia.app.data.SampleData

import com.puraguia.app.ui.profile.ActividadRecienteActivity
import com.puraguia.app.ui.profile.AgregarTourActivity
import com.puraguia.app.ui.profile.ConfiguracionActivity
import com.puraguia.app.ui.profile.FavoritosActivity
import com.puraguia.app.ui.profile.MetodosPagoActivity
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.iniciales
import com.puraguia.app.util.toast

/**
 * Pestaña Perfil:
 * datos de sesión, accesos, agregar tour
 * e historial de reservas.
 */
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())

        // Datos del perfil
        view.findViewById<TextView>(R.id.txt_nombre).text =
            session.getNombre()

        view.findViewById<TextView>(R.id.txt_email).text =
            session.getEmail()

        view.findViewById<TextView>(R.id.txt_avatar).text =
            iniciales(session.getNombre()).take(1)

        view.findViewById<TextView>(R.id.txt_rol).text =
            if (session.esGuia()) "Guía" else "Turista"

        // AGREGAR TOUR SOLO PARA GUÍAS
        val rowAgregarTour =
            view.findViewById<View>(R.id.row_agregar_tour)

        rowAgregarTour.visibility =
            if (session.esGuia()) View.VISIBLE else View.GONE

        view.findViewById<View>(R.id.divider_agregar_tour).visibility =
            if (session.esGuia()) View.VISIBLE else View.GONE


        rowAgregarTour.setOnClickListener {
            val intent = Intent(
                requireContext(),
                AgregarTourActivity::class.java
            )
            startActivity(intent)
        }


        // Estadísticas de ejemplo
        view.findViewById<TextView>(R.id.txt_num_reservas).text =
            SampleData.historialReservas.size.toString()

        view.findViewById<TextView>(R.id.txt_num_resenas).text = "3"

        // Pago
        view.findViewById<View>(R.id.row_pago).setOnClickListener {
            startActivity(
                Intent(requireContext(), MetodosPagoActivity::class.java)
            )
        }

        // Favoritos
        view.findViewById<View>(R.id.row_favoritos).setOnClickListener {
            startActivity(
                Intent(requireContext(), FavoritosActivity::class.java)
            )
        }

        // Configuración
        view.findViewById<View>(R.id.row_configuracion).setOnClickListener {
            startActivity(
                Intent(requireContext(), ConfiguracionActivity::class.java)
            )
        }

        // Recientes
        view.findViewById<View>(R.id.row_recientes).setOnClickListener {
            startActivity(
                Intent(requireContext(), ActividadRecienteActivity::class.java)
            )
        }

        // Historial de reservas (ejemplo)
        val contenedor =
            view.findViewById<LinearLayout>(R.id.container_historial)

        contenedor.removeAllViews()

        SampleData.historialReservas.forEach { reserva ->

            val fila = layoutInflater.inflate(
                R.layout.item_historial,
                contenedor,
                false
            )

            fila.findViewById<TextView>(R.id.txt_titulo).text =
                reserva.tour

            fila.findViewById<TextView>(R.id.txt_estado).text =
                reserva.estado

            fila.findViewById<TextView>(R.id.txt_fecha).text =
                reserva.fecha

            fila.findViewById<TextView>(R.id.txt_total).text =
                "$${reserva.total}"

            contenedor.addView(fila)
        }

        // Cerrar sesión
        view.findViewById<View>(R.id.btn_cerrar_sesion).setOnClickListener {
            confirmarCerrarSesion(session)
        }
    }



    // CERRAR SESIÓN
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
