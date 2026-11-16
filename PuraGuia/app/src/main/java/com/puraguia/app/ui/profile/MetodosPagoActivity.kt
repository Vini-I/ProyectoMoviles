package com.puraguia.app.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.SampleData
import com.puraguia.app.model.Tarjeta
import com.puraguia.app.util.setupHeader
import com.puraguia.app.util.toast

/**
 * Pantalla "Métodos de Pago": muestra las tarjetas guardadas y permite agregar o quitar.
 *
 * TODO(equipo pagos): guardar y consultar las tarjetas en SQLite (tabla `metodos_pago`).
 */
class MetodosPagoActivity : AppCompatActivity() {

    private lateinit var contenedor: LinearLayout
    private lateinit var txtSinTarjetas: TextView
    private val tarjetas: MutableList<Tarjeta> = SampleData.tarjetas.toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metodos_pago)
        setupHeader("Métodos de Pago")

        contenedor = findViewById(R.id.container_tarjetas)
        txtSinTarjetas = findViewById(R.id.txt_sin_tarjetas)

        findViewById<View>(R.id.btn_agregar_tarjeta).setOnClickListener {
            // TODO(equipo pagos): abrir formulario para agregar tarjeta y guardarla en SQLite
            toast("Agregar tarjeta pendiente (equipo pagos)")
        }

        mostrarTarjetas()
    }

    private fun mostrarTarjetas() {
        contenedor.removeAllViews()
        txtSinTarjetas.visibility = if (tarjetas.isEmpty()) View.VISIBLE else View.GONE

        tarjetas.toList().forEach { tarjeta ->
            val vista = layoutInflater.inflate(R.layout.item_tarjeta, contenedor, false)
            val txtMarca = vista.findViewById<TextView>(R.id.txt_marca)
            txtMarca.text = tarjeta.marca
            txtMarca.setBackgroundResource(
                if (tarjeta.marca.equals("MC", ignoreCase = true)) {
                    R.drawable.bg_brand_mc
                } else {
                    R.drawable.bg_brand_tag
                }
            )
            vista.findViewById<View>(R.id.txt_predeterminada).visibility =
                if (tarjeta.predeterminada) View.VISIBLE else View.GONE
            vista.findViewById<TextView>(R.id.txt_numero).text =
                "•••• •••• •••• ${tarjeta.ultimos4}"
            vista.findViewById<TextView>(R.id.txt_vence).text = "Vence ${tarjeta.vence}"

            vista.findViewById<View>(R.id.btn_eliminar).setOnClickListener {
                confirmarEliminar(tarjeta)
            }
            contenedor.addView(vista)
        }
    }

    private fun confirmarEliminar(tarjeta: Tarjeta) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tarjeta")
            .setMessage("¿Deseas eliminar la tarjeta terminada en ${tarjeta.ultimos4}?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                // TODO(equipo pagos): eliminar en SQLite
                tarjetas.remove(tarjeta)
                toast("Tarjeta eliminada")
                mostrarTarjetas()
            }
            .show()
    }
}
