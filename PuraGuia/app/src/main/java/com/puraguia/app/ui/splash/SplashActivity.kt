package com.puraguia.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.ui.main.MainActivity

/** Pantalla de presentación: logo + nombre y, tras unos segundos, pasa sola a la app. */
class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val contenido = findViewById<View>(R.id.splash_content)
        contenido.alpha = 0f
        contenido.animate().alpha(1f).setDuration(900).start()

        handler.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, SPLASH_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val SPLASH_MS = 2200L
    }
}
