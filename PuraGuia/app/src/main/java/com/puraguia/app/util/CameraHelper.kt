package com.puraguia.app.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object CameraHelper {

    const val REQUEST_CODE_CAMERA = 100

    /** Comprueba si el permiso de cámara está concedido */
    fun tienePermisoCamara(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Solicita el permiso para utilizar la cámara */
    fun solicitarPermisoCamara(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CODE_CAMERA
        )
    }

    /** Comprueba si Android recomienda mostrar una explicación */
    fun debeMostrarJustificacion(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.CAMERA
        )
    }
}