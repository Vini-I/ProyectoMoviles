package com.puraguia.app.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.TourDAO
import com.puraguia.app.util.FileUtils
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.toast
import java.io.File

/**
 * Formulario para agregar un nuevo tour o editar un tour existente en SQLite.
 */
class AgregarTourActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var tourDao: TourDAO

    private lateinit var editTitulo: EditText
    private lateinit var editLugar: EditText
    private lateinit var editPrecio: EditText
    private lateinit var spinnerDuracion: Spinner
    private lateinit var editCuidados: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var imgTour: ImageView

    private var tourIdEditar: Int = -1
    private var imagenPath: String? = null

    private val categorias = listOf(
        "Aventura",
        "Naturaleza",
        "Avistamiento",
        "Familiar",
        "Cultura",
        "Playa"
    )

    private val duraciones = listOf(
        "Seleccionar duración",
        "30 minutos",
        "1 hora",
        "2 horas",
        "3 horas",
        "4 horas",
        "5 horas",
        "6 horas",
        "8 horas",
        "Día completo"
    )

    private val seleccionarImagen =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val ruta = FileUtils.copiarAInterno(this, uri, "tours")
                if (ruta != null) {
                    imagenPath = ruta
                    imgTour.setImageURI(Uri.fromFile(File(ruta)))
                } else {
                    toast("No se pudo guardar la imagen seleccionada")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_agregar_tour)
        supportActionBar?.hide()

        session = SessionManager(this)
        tourDao = TourDAO(this)

        if (!session.esGuia()) {
            toast("Solo los guías pueden gestionar tours")
            finish()
            return
        }

        tourIdEditar = intent.getIntExtra(EXTRA_TOUR_ID, -1)

        editTitulo = findViewById(R.id.et_tour_titulo)
        editLugar = findViewById(R.id.et_tour_lugar)
        editPrecio = findViewById(R.id.et_tour_precio)
        spinnerDuracion = findViewById(R.id.sp_tour_duracion)
        editCuidados = findViewById(R.id.et_tour_cuidados)
        spinnerCategoria = findViewById(R.id.sp_tour_categoria)
        imgTour = findViewById(R.id.img_tour)

        findViewById<Button>(R.id.btn_imagen).setOnClickListener {
            seleccionarImagen.launch("image/*")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapter

        val adapterDuracion = ArrayAdapter(this, android.R.layout.simple_spinner_item, duraciones)
        adapterDuracion.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDuracion.adapter = adapterDuracion

        if (tourIdEditar > 0) {
            cargarTourParaEditar(tourIdEditar)
        }

        findViewById<Button>(R.id.btn_guardar_tour).setOnClickListener { guardarTour() }
        findViewById<Button>(R.id.btn_cancelar_tour).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btn_volver_tour).setOnClickListener { finish() }
    }

    private fun cargarTourParaEditar(id: Int) {
        val tour = tourDao.obtenerPorId(id)
        if (tour == null) {
            toast("No se encontró el tour")
            finish()
            return
        }
        findViewById<TextView>(R.id.txt_titulo_formulario).text = "Editar Tour"
        findViewById<TextView>(R.id.txt_subtitulo_formulario).text =
            "Actualiza la información del tour seleccionado."
        findViewById<Button>(R.id.btn_guardar_tour).text = "Actualizar Tour"

        editTitulo.setText(tour.titulo)
        editLugar.setText(tour.lugar)
        editPrecio.setText(tour.precio.toString())
        editCuidados.setText(tour.cuidados)
        imagenPath = tour.imagenPath

        if (!imagenPath.isNullOrBlank() && File(imagenPath!!).exists()) {
            imgTour.setImageURI(Uri.fromFile(File(imagenPath!!)))
        } else {
            imgTour.setImageResource(tour.imagen)
        }

        val idxCat = categorias.indexOfFirst { it.equals(tour.categoria, ignoreCase = true) }
        if (idxCat >= 0) spinnerCategoria.setSelection(idxCat)

        val idxDur = duraciones.indexOfFirst {
            tour.duracion.startsWith(it, ignoreCase = true) || it.equals(tour.duracion, ignoreCase = true)
        }
        if (idxDur > 0) {
            spinnerDuracion.setSelection(idxDur)
        } else {
            // Si la duración original tenía horario detallado (ej. "8 horas (9:00 AM - 5:00 PM)"), buscamos coincidencia numérica
            val horas = Regex("\\d+").find(tour.duracion)?.value
            val idxPorHora = if (horas != null) {
                duraciones.indexOfFirst { it.startsWith("$horas ") }
            } else -1
            if (idxPorHora > 0) spinnerDuracion.setSelection(idxPorHora)
        }
    }

    private fun guardarTour() {
        val titulo = editTitulo.text.toString().trim()
        val lugar = editLugar.text.toString().trim()
        val precio = editPrecio.text.toString().toIntOrNull()
        val duracion = spinnerDuracion.selectedItem.toString().trim()
        val cuidados = editCuidados.text.toString().trim()
        val categoria = spinnerCategoria.selectedItem.toString()

        if (titulo.isEmpty()) {
            editTitulo.error = "Ingrese el nombre"
            editTitulo.requestFocus()
            return
        }
        if (lugar.isEmpty()) {
            editLugar.error = "Ingrese el lugar"
            editLugar.requestFocus()
            return
        }
        if (precio == null || precio <= 0) {
            editPrecio.error = "Precio inválido"
            editPrecio.requestFocus()
            return
        }
        if (spinnerDuracion.selectedItemPosition == 0) {
            toast("Seleccione la duración del tour")
            return
        }
        if (cuidados.isEmpty()) {
            editCuidados.error = "Ingrese las recomendaciones"
            editCuidados.requestFocus()
            return
        }

        try {
            if (tourIdEditar > 0) {
                val actualizado = tourDao.actualizar(
                    id = tourIdEditar,
                    titulo = titulo,
                    lugar = lugar,
                    precio = precio,
                    categoria = categoria,
                    duracion = duracion,
                    cuidados = cuidados,
                    imagenPath = imagenPath
                )
                if (actualizado) {
                    toast("Tour actualizado correctamente")
                    setResult(RESULT_OK)
                    finish()
                } else {
                    toast("No se pudo actualizar el tour")
                }
            } else {
                val id = tourDao.insertar(
                    guiaId = session.getUserId(),
                    titulo = titulo,
                    lugar = lugar,
                    precio = precio,
                    categoria = categoria,
                    duracion = duracion,
                    cuidados = cuidados,
                    imagenPath = imagenPath
                )
                if (id > 0) {
                    toast("Tour agregado correctamente")
                    setResult(RESULT_OK)
                    finish()
                } else {
                    toast("Error al guardar el tour")
                }
            }
        } catch (e: Exception) {
            Log.e("AgregarTourActivity", "Error al guardar tour", e)
            toast("Error al guardar el tour")
        }
    }

    companion object {
        private const val EXTRA_TOUR_ID = "tour_id"

        fun createEditIntent(context: Context, tourId: Int): Intent =
            Intent(context, AgregarTourActivity::class.java).putExtra(EXTRA_TOUR_ID, tourId)
    }
}
