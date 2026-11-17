package com.puraguia.app.ui.profile

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.ActividadDao
import com.puraguia.app.model.Actividad
import com.puraguia.app.model.TipoActividad
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.dp
import com.puraguia.app.util.setupHeader

/**
 * Pantalla "Actividad Reciente": historial de acciones guardado por usuario en SQLite.
 */
class ActividadRecienteActivity : AppCompatActivity() {

    private lateinit var actividadDao: ActividadDao
    private lateinit var session: SessionManager
    private lateinit var contenedor: LinearLayout
    private lateinit var tabs: List<Pair<TextView, TipoActividad?>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actividad_reciente)
        setupHeader("Actividad Reciente", "Últimas acciones en tu cuenta")

        actividadDao = ActividadDao(this)
        session = SessionManager(this)
        contenedor = findViewById(R.id.container_actividades)
        tabs = listOf(
            findViewById<TextView>(R.id.tab_todo) to null,
            findViewById<TextView>(R.id.tab_reservas) to TipoActividad.RESERVA,
            findViewById<TextView>(R.id.tab_favoritos) to TipoActividad.FAVORITO,
            findViewById<TextView>(R.id.tab_resenas) to TipoActividad.RESENA
        )

        tabs.forEach { (tab, filtro) ->
            tab.setOnClickListener { seleccionarTab(tab, filtro) }
        }
        seleccionarTab(tabs.first().first, null)
    }

    private fun seleccionarTab(tabActivo: TextView, filtro: TipoActividad?) {
        tabs.forEach { (tab, _) -> tab.isSelected = (tab == tabActivo) }
        val lista = actividadDao.obtenerPorUsuario(session.getUserId(), filtro)
        mostrarActividades(lista)
    }

    private fun mostrarActividades(actividades: List<Actividad>) {
        contenedor.removeAllViews()
        if (actividades.isEmpty()) {
            val vacio = TextView(this).apply {
                text = "Aún no hay actividad registrada en esta categoría."
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(Color.parseColor("#6B7280"))
                setPadding(dp(16), dp(32), dp(16), dp(16))
            }
            contenedor.addView(vacio)
            return
        }

        actividades.forEach { actividad ->
            val vista = layoutInflater.inflate(R.layout.item_actividad, contenedor, false)
            val img = vista.findViewById<ImageView>(R.id.img_icono)
            val (fondo, icono, colorTint) = estiloPorTipo(actividad.tipo)
            img.setBackgroundResource(fondo)
            img.setImageResource(icono)
            img.setColorFilter(Color.parseColor(colorTint))

            vista.findViewById<TextView>(R.id.txt_titulo).text = actividad.titulo
            vista.findViewById<TextView>(R.id.txt_detalle).text = actividad.detalle
            vista.findViewById<TextView>(R.id.txt_fecha).text = actividad.fecha
            contenedor.addView(vista)
        }
    }

    private fun estiloPorTipo(tipo: TipoActividad): Triple<Int, Int, String> = when (tipo) {
        TipoActividad.RESERVA -> Triple(R.drawable.bg_act_green, R.drawable.ic_check_circle, "#16A34A")
        TipoActividad.FAVORITO -> Triple(R.drawable.bg_act_red, R.drawable.ic_favorite, "#DC2626")
        TipoActividad.RESENA -> Triple(R.drawable.bg_act_amber, R.drawable.ic_star, "#D97706")
        TipoActividad.BUSQUEDA -> Triple(R.drawable.bg_act_blue, R.drawable.ic_search, "#0284C7")
        TipoActividad.PAGO -> Triple(R.drawable.bg_act_purple, R.drawable.ic_credit_card, "#9333EA")
        TipoActividad.SESION -> Triple(R.drawable.bg_act_teal, R.drawable.ic_key, "#0D9488")
    }
}
