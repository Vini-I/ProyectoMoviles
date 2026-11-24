package com.puraguia.app.ui.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.puraguia.app.R
import com.puraguia.app.data.TourDAO
import com.puraguia.app.util.CameraHelper
import com.puraguia.app.util.FileUtils
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.setupHeader
import com.puraguia.app.util.toast
import java.io.File

/**
 * Formulario para agregar un nuevo tour o editar un tour existente en SQLite,
 * con soporte para galería y captura directa con la cámara del dispositivo.
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
    private lateinit var txtEstadoFoto: TextView

    private var tourIdEditar: Int = -1
    private var imagenPath: String? = null
    private var cameraImagePath: String? = null
    private var estadoFotoActual: String = ESTADO_PENDIENTE

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
            if (uri == null) return@registerForActivityResult
            val ruta = FileUtils.copiarAInterno(this, uri, "tours")
            if (ruta != null) {
                imagenPath = ruta
                mostrarFotoLocal(ruta)
                actualizarEstadoFoto(ESTADO_TOMADA)
                toast("Imagen seleccionada correctamente")
            } else {
                actualizarEstadoFoto(ESTADO_ERROR)
                toast("No se pudo guardar la imagen seleccionada")
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val archivoTemp = cameraImagePath?.let { File(it) }
            cameraImagePath = null

            if (success && archivoTemp != null && archivoTemp.exists() && archivoTemp.length() > 0L) {
                val ruta = FileUtils.copiarAInterno(this, Uri.fromFile(archivoTemp), "tours")
                archivoTemp.delete()
                if (ruta != null) {
                    imagenPath = ruta
                    mostrarFotoLocal(ruta)
                    actualizarEstadoFoto(ESTADO_TOMADA)
                    toast("Fotografía capturada correctamente")
                } else {
                    actualizarEstadoFoto(ESTADO_ERROR)
                    toast("No se pudo procesar la fotografía tomada")
                }
            } else {
                archivoTemp?.delete()
                actualizarEstadoFoto(ESTADO_ERROR)
                toast("Captura cancelada o no disponible")
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                lanzarCamara()
            } else {
                actualizarEstadoFoto(ESTADO_ERROR)
                if (!CameraHelper.debeMostrarJustificacion(this)) {
                    mostrarDialogoPermisoBloqueado()
                } else {
                    toast("Permiso de cámara denegado")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_agregar_tour)
        setupHeader("Agregar Tour", "Completa la información para publicar un nuevo tour")

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
        txtEstadoFoto = findViewById(R.id.txt_estado_foto)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapter

        val adapterDuracion = ArrayAdapter(this, android.R.layout.simple_spinner_item, duraciones)
        adapterDuracion.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDuracion.adapter = adapterDuracion

        if (savedInstanceState != null) {
            imagenPath = savedInstanceState.getString(STATE_IMAGEN_PATH)
            cameraImagePath = savedInstanceState.getString(STATE_CAMERA_PATH)
            estadoFotoActual = savedInstanceState.getString(STATE_ESTADO_FOTO, ESTADO_PENDIENTE)
        }

        if (tourIdEditar > 0 && savedInstanceState == null) {
            cargarTourParaEditar(tourIdEditar)
        } else {
            if (tourIdEditar > 0) {
                configurarEncabezadoEdicion()
            }
            if (!imagenPath.isNullOrBlank() && File(imagenPath!!).exists()) {
                mostrarFotoLocal(imagenPath!!)
            }
            actualizarEstadoFoto(estadoFotoActual)
        }

        findViewById<Button>(R.id.btn_imagen).setOnClickListener {
            seleccionarImagen.launch("image/*")
        }

        findViewById<Button>(R.id.btn_tomar_foto).setOnClickListener {
            iniciarFlujoCamara()
        }

        findViewById<Button>(R.id.btn_guardar_tour).setOnClickListener { guardarTour() }
        findViewById<Button>(R.id.btn_cancelar_tour).setOnClickListener { finish() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_IMAGEN_PATH, imagenPath)
        outState.putString(STATE_CAMERA_PATH, cameraImagePath)
        outState.putString(STATE_ESTADO_FOTO, estadoFotoActual)
    }

    private fun iniciarFlujoCamara() {
        when {
            CameraHelper.tienePermisoCamara(this) -> lanzarCamara()
            CameraHelper.debeMostrarJustificacion(this) -> {
                AlertDialog.Builder(this)
                    .setTitle("Permiso de cámara requerido")
                    .setMessage("Pura Guía necesita acceso a la cámara para tomar la fotografía del tour.")
                    .setNegativeButton("Cancelar") { _, _ ->
                        actualizarEstadoFoto(ESTADO_ERROR)
                        toast("Permiso de cámara denegado")
                    }
                    .setPositiveButton("Continuar") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .show()
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun lanzarCamara() {
        try {
            val archivo = FileUtils.crearArchivoTemporal(this, "camara", "tour_", ".jpg")
            cameraImagePath = archivo.absolutePath
            val uri = FileUtils.obtenerUriFileProvider(this, archivo)
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Log.e("AgregarTourActivity", "Error al iniciar la cámara", e)
            cameraImagePath = null
            actualizarEstadoFoto(ESTADO_ERROR)
            toast("No hay una cámara disponible en este dispositivo")
        }
    }

    private fun mostrarDialogoPermisoBloqueado() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de cámara desactivado")
            .setMessage("Debes habilitar el permiso de cámara en la configuración de la aplicación para tomar fotografías.")
            .setNegativeButton("Cerrar", null)
            .setPositiveButton("Abrir ajustes") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
                startActivity(intent)
            }
            .show()
    }

    private fun mostrarFotoLocal(ruta: String) {
        imgTour.imageTintList = null
        imgTour.setPadding(0, 0, 0, 0)
        imgTour.scaleType = ImageView.ScaleType.CENTER_CROP
        imgTour.setImageURI(Uri.fromFile(File(ruta)))
    }

    private fun actualizarEstadoFoto(estado: String) {
        estadoFotoActual = estado
        txtEstadoFoto.text = estado
        when (estado) {
            ESTADO_TOMADA -> {
                txtEstadoFoto.setBackgroundResource(R.drawable.bg_badge_ok)
                txtEstadoFoto.setTextColor(ContextCompat.getColor(this, R.color.pg_green))
            }
            ESTADO_ERROR -> {
                txtEstadoFoto.setBackgroundResource(R.drawable.bg_badge_error)
                txtEstadoFoto.setTextColor(ContextCompat.getColor(this, R.color.pg_red))
            }
            else -> {
                txtEstadoFoto.setBackgroundResource(R.drawable.bg_badge_neutral)
                txtEstadoFoto.setTextColor(ContextCompat.getColor(this, R.color.pg_text_muted))
            }
        }
    }

    private fun configurarEncabezadoEdicion() {
        setupHeader("Editar Tour", "Actualiza la información del tour seleccionado")
        findViewById<Button>(R.id.btn_guardar_tour).text = "Actualizar Tour"
    }

    private fun cargarTourParaEditar(id: Int) {
        val tour = tourDao.obtenerPorId(id)
        if (tour == null) {
            toast("No se encontró el tour")
            finish()
            return
        }
        configurarEncabezadoEdicion()

        editTitulo.setText(tour.titulo)
        editLugar.setText(tour.lugar)
        editPrecio.setText(tour.precio.toString())
        editCuidados.setText(tour.cuidados)
        imagenPath = tour.imagenPath

        if (!imagenPath.isNullOrBlank() && File(imagenPath!!).exists()) {
            mostrarFotoLocal(imagenPath!!)
            actualizarEstadoFoto(ESTADO_TOMADA)
        } else {
            imgTour.imageTintList = null
            imgTour.setPadding(0, 0, 0, 0)
            imgTour.scaleType = ImageView.ScaleType.CENTER_CROP
            imgTour.setImageResource(tour.imagen)
            actualizarEstadoFoto(ESTADO_PENDIENTE)
        }

        val idxCat = categorias.indexOfFirst { it.equals(tour.categoria, ignoreCase = true) }
        if (idxCat >= 0) spinnerCategoria.setSelection(idxCat)

        val idxDur = duraciones.indexOfFirst {
            tour.duracion.startsWith(it, ignoreCase = true) || it.equals(tour.duracion, ignoreCase = true)
        }
        if (idxDur > 0) {
            spinnerDuracion.setSelection(idxDur)
        } else {
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
        private const val STATE_IMAGEN_PATH = "state_imagen_path"
        private const val STATE_CAMERA_PATH = "state_camera_path"
        private const val STATE_ESTADO_FOTO = "state_estado_foto"

        private const val ESTADO_PENDIENTE = "Pendiente"
        private const val ESTADO_TOMADA = "Tomada"
        private const val ESTADO_ERROR = "Error"

        fun createEditIntent(context: Context, tourId: Int): Intent =
            Intent(context, AgregarTourActivity::class.java).putExtra(EXTRA_TOUR_ID, tourId)
    }
}
