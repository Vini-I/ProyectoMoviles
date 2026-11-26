package com.puraguia.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import java.util.Locale

/**
 * Helper reutilizable para gestionar permisos y lectura de ubicación GPS real del dispositivo.
 * Utiliza las APIs estándar de Android (LocationManager + LocationManagerCompat) sin dependencias externas.
 */
object LocationHelper {

    val PERMISOS_UBICACION = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /** Comprueba si el usuario otorgó permiso de ubicación precisa o aproximada. */
    fun tienePermisoUbicacion(context: Context): Boolean {
        val fino = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val aproximado = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fino || aproximado
    }

    /** Comprueba si está concedido el permiso de ubicación precisa (GPS). */
    fun tienePermisoPreciso(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Comprueba si Android recomienda mostrar una explicación previa antes de pedir el permiso. */
    fun debeMostrarJustificacion(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /** Verifica si los servicios de ubicación del dispositivo están habilitados. */
    fun estaUbicacionHabilitada(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    /**
     * Solicita la ubicación actual real del dispositivo/emulador.
     * Devuelve un CancellationSignal para cancelar la lectura si la pantalla se cierra.
     */
    @SuppressLint("MissingPermission")
    fun obtenerUbicacionActual(
        context: Context,
        onExito: (latitud: Double, longitud: Double) -> Unit,
        onError: (mensaje: String) -> Unit
    ): CancellationSignal? {
        if (!tienePermisoUbicacion(context)) {
            onError("Permiso de ubicación requerido")
            return null
        }

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            onError("El dispositivo no cuenta con servicio de ubicación")
            return null
        }

        if (!LocationManagerCompat.isLocationEnabled(manager)) {
            onError("Activa el GPS o la ubicación del dispositivo")
            return null
        }

        val proveedor = seleccionarProveedor(context, manager)
        if (proveedor == null) {
            val respaldo = obtenerUltimaUbicacion(context, manager)
            if (respaldo != null) {
                onExito(respaldo.latitude, respaldo.longitude)
            } else {
                onError("No hay proveedores de ubicación disponibles en este dispositivo")
            }
            return null
        }

        val cancelSignal = CancellationSignal()
        return try {
            LocationManagerCompat.getCurrentLocation(
                manager,
                proveedor,
                cancelSignal,
                ContextCompat.getMainExecutor(context)
            ) { ubicacion ->
                if (ubicacion != null) {
                    onExito(ubicacion.latitude, ubicacion.longitude)
                } else {
                    val respaldo = obtenerUltimaUbicacion(context, manager)
                    if (respaldo != null) {
                        onExito(respaldo.latitude, respaldo.longitude)
                    } else {
                        onError("No se pudo obtener la ubicación actual. Intenta nuevamente.")
                    }
                }
            }
            cancelSignal
        } catch (e: SecurityException) {
            onError("Permiso de ubicación denegado")
            null
        } catch (e: Exception) {
            val respaldo = obtenerUltimaUbicacion(context, manager)
            if (respaldo != null) {
                onExito(respaldo.latitude, respaldo.longitude)
            } else {
                onError("Error al consultar la ubicación del dispositivo")
            }
            null
        }
    }

    /** Formatea latitud y longitud con 5 decimales usando punto decimal estándar. */
    fun formatearCoordenadas(latitud: Double, longitud: Double): String {
        return String.format(Locale.US, "%.5f, %.5f", latitud, longitud)
    }

    private fun seleccionarProveedor(context: Context, manager: LocationManager): String? {
        val gpsActivo = runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }
            .getOrDefault(false)
        val redActiva = runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }
            .getOrDefault(false)

        return when {
            tienePermisoPreciso(context) && gpsActivo -> LocationManager.GPS_PROVIDER
            redActiva -> LocationManager.NETWORK_PROVIDER
            gpsActivo -> LocationManager.GPS_PROVIDER
            else -> null
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUltimaUbicacion(context: Context, manager: LocationManager): Location? {
        if (!tienePermisoUbicacion(context)) return null
        val proveedores = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return proveedores.mapNotNull { proveedor ->
            runCatching { manager.getLastKnownLocation(proveedor) }.getOrNull()
        }.maxByOrNull { it.time }
    }
}
