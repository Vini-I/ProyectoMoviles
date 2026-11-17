package com.puraguia.app.ui.tour

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.puraguia.app.R
import com.puraguia.app.data.ActividadDao
import com.puraguia.app.data.ReservaDao
import com.puraguia.app.data.TourDAO
import com.puraguia.app.model.TipoActividad
import com.puraguia.app.model.Tour
import com.puraguia.app.util.ChipGrid
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.dp
import com.puraguia.app.util.setupHeader
import com.puraguia.app.util.toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pantalla de reserva: día, hora, método de pago y resumen, persistida en SQLite.
 */
class ReservaActivity : AppCompatActivity() {

    private lateinit var tourDao: TourDAO
    private lateinit var reservaDao: ReservaDao
    private lateinit var actividadDao: ActividadDao
    private lateinit var session: SessionManager

    private lateinit var tour: Tour
    private lateinit var spinnerHora: Spinner
    private lateinit var btnAm: TextView
    private lateinit var btnPm: TextView
    private lateinit var btnConfirmar: View
    private lateinit var txtResumenDia: TextView
    private lateinit var txtResumenHora: TextView

    private val celdasDias = mutableListOf<TextView>()
    private var diaSeleccionado: Calendar? = null
    private var metodoPago: String? = null

    private val formatoDia = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "CR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reserva)

        tourDao = TourDAO(this)
        reservaDao = ReservaDao(this)
        actividadDao = ActividadDao(this)
        session = SessionManager(this)

        val id = intent.getIntExtra(EXTRA_TOUR_ID, -1)
        val encontrado = tourDao.obtenerPorId(id) ?: tourDao.obtenerTodos().firstOrNull()
        if (encontrado == null) {
            toast("Tour no disponible")
            finish()
            return
        }
        tour = encontrado
        setupHeader("Reservar Tour", tour.titulo)

        spinnerHora = findViewById(R.id.spinner_hora)
        btnAm = findViewById(R.id.btn_am)
        btnPm = findViewById(R.id.btn_pm)
        btnConfirmar = findViewById(R.id.btn_confirmar_reserva)
        txtResumenDia = findViewById(R.id.txt_resumen_dia)
        txtResumenHora = findViewById(R.id.txt_resumen_hora)

        construirDias()
        configurarHora()
        configurarPago()

        findViewById<TextView>(R.id.txt_resumen_duracion).text = tour.duracion.substringBefore(" (")
        findViewById<TextView>(R.id.txt_resumen_guia).text = tour.guia
        findViewById<TextView>(R.id.txt_total).text = "$${tour.precio}"

        btnConfirmar.setOnClickListener { confirmarReserva() }
        actualizarResumen()
    }

    private fun confirmarReserva() {
        val dia = diaSeleccionado
        val metodo = metodoPago
        val posHora = spinnerHora.selectedItemPosition
        if (dia == null || posHora <= 0 || metodo == null) {
            toast("Completa fecha, hora y método de pago")
            return
        }
        val fechaTexto = formatoDia.format(dia.time).replaceFirstChar { it.uppercase() }
        val horaTexto = "${spinnerHora.selectedItem} ${if (btnAm.isSelected) "AM" else "PM"}"

        val idReserva = reservaDao.insertar(
            usuarioId = session.getUserId(),
            tourId = tour.id,
            fechaTour = fechaTexto,
            horaTour = horaTexto,
            metodoPago = metodo,
            total = tour.precio
        )
        if (idReserva > 0L) {
            actividadDao.registrar(
                session.getUserId(),
                TipoActividad.RESERVA,
                "Reserva confirmada",
                "${tour.titulo} · $fechaTexto ($horaTexto)"
            )
            AlertDialog.Builder(this)
                .setTitle("¡Reserva Confirmada!")
                .setMessage("Tu reserva para \"${tour.titulo}\" el $fechaTexto a las $horaTexto quedó guardada.")
                .setPositiveButton("Aceptar") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        } else {
            toast("No se pudo guardar la reserva")
        }
    }

    private fun inicioDelDia(calendario: Calendar): Calendar {
        calendario.set(Calendar.HOUR_OF_DAY, 0)
        calendario.set(Calendar.MINUTE, 0)
        calendario.set(Calendar.SECOND, 0)
        calendario.set(Calendar.MILLISECOND, 0)
        return calendario
    }

    /** Muestra 4 semanas a partir de la semana actual (domingo a sábado). */
    private fun construirDias() {
        val grid = findViewById<GridLayout>(R.id.grid_dias)
        grid.removeAllViews()
        grid.columnCount = 7

        val nombres = listOf("dom", "lun", "mar", "mié", "jue", "vie", "sáb")
        val hoy = inicioDelDia(Calendar.getInstance())
        val primerDia = inicioDelDia(Calendar.getInstance())
        primerDia.add(Calendar.DAY_OF_YEAR, -(primerDia.get(Calendar.DAY_OF_WEEK) - 1))

        for (indice in 0 until 28) {
            val dia = primerDia.clone() as Calendar
            dia.add(Calendar.DAY_OF_YEAR, indice)
            val pasado = dia.before(hoy)

            val celda = TextView(this)
            celda.text = "${nombres[dia.get(Calendar.DAY_OF_WEEK) - 1]}\n${dia.get(Calendar.DAY_OF_MONTH)}"
            celda.gravity = Gravity.CENTER
            celda.textSize = 13f
            celda.setBackgroundResource(R.drawable.bg_chip)
            celda.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text))

            if (pasado) {
                celda.alpha = 0.35f
                celda.isEnabled = false
            } else {
                celda.isClickable = true
                celda.isFocusable = true
                celda.setOnClickListener {
                    celdasDias.forEach { it.isSelected = false }
                    celda.isSelected = true
                    diaSeleccionado = dia
                    actualizarResumen()
                }
            }

            val parametros = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f)
            )
            parametros.width = 0
            parametros.height = dp(56)
            parametros.setMargins(dp(3), dp(3), dp(3), dp(3))
            celdasDias.add(celda)
            grid.addView(celda, parametros)
        }
    }

    private fun configurarHora() {
        val horas = mutableListOf("Hora")
        for (hora in 1..12) horas.add("$hora:00")
        val adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_item, horas)
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerHora.adapter = adaptador
        spinnerHora.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                actualizarResumen()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnAm.isSelected = true
        btnAm.setOnClickListener {
            btnAm.isSelected = true
            btnPm.isSelected = false
            actualizarResumen()
        }
        btnPm.setOnClickListener {
            btnPm.isSelected = true
            btnAm.isSelected = false
            actualizarResumen()
        }
    }

    private fun configurarPago() {
        ChipGrid(
            this,
            findViewById<GridLayout>(R.id.grid_pago),
            listOf("Sinpe", "Tarjeta", "PayPal"),
            seleccionMultiple = false,
            columnas = 2,
            alturaDp = 92,
            iconos = listOf(R.drawable.ic_credit_card, R.drawable.ic_credit_card, R.drawable.ic_wallet),
            alCambiar = { elegidos ->
                metodoPago = elegidos.firstOrNull()
                actualizarResumen()
            }
        )
    }

    private fun actualizarResumen() {
        val dia = diaSeleccionado
        txtResumenDia.text = if (dia != null) {
            formatoDia.format(dia.time).replaceFirstChar { it.uppercase() }
        } else {
            "No seleccionado"
        }

        val posicionHora = spinnerHora.selectedItemPosition
        txtResumenHora.text = if (posicionHora > 0) {
            "${spinnerHora.selectedItem} ${if (btnAm.isSelected) "AM" else "PM"}"
        } else {
            "No seleccionada"
        }

        val completo = dia != null && posicionHora > 0 && metodoPago != null
        btnConfirmar.isEnabled = completo
        btnConfirmar.alpha = if (completo) 1f else 0.5f
    }

    companion object {
        private const val EXTRA_TOUR_ID = "tour_id"

        fun createIntent(context: Context, tourId: Int): Intent =
            Intent(context, ReservaActivity::class.java).putExtra(EXTRA_TOUR_ID, tourId)
    }
}
