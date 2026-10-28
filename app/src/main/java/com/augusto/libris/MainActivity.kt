package com.augusto.libris

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.augusto.libris.di.AppContainer
import com.augusto.libris.ui.view.LibraryApp
import com.augusto.libris.ui.auth.FirebaseGate
import com.augusto.libris.ui.theme.LibrisTheme

class LibraryApplication : Application(), ImageLoaderFactory {
    val container: AppContainer by lazy { AppContainer(this) }

    // Permite reutilizar portadas ya descargadas incluso con cabeceras HTTP caducadas.
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .respectCacheHeaders(false)
        .build()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibrisTheme {
                FirebaseGate { LibraryApp() }
            }
        }
    }
}
