
package com.augusto.libris.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.augusto.libris.BuildConfig
import com.augusto.libris.data.local.LibraryDatabase
import com.augusto.libris.data.remote.GoogleBooksApi
import com.augusto.libris.data.remote.GutendexApi
import com.augusto.libris.data.remote.OpenLibraryApi
import com.augusto.libris.data.remote.LibrisCloudApi
import com.augusto.libris.data.repository.AuthorRepository
import com.augusto.libris.data.repository.BookRepository
import com.augusto.libris.data.repository.FavoritesRepository
import com.augusto.libris.data.repository.GutenbergRepository
import com.augusto.libris.ui.viewmodel.AuthorViewModel
import com.augusto.libris.ui.viewmodel.BooksViewModel
import com.augusto.libris.ui.viewmodel.FavoritesViewModel
import com.augusto.libris.ui.viewmodel.ReadingViewModel
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    // Base de datos local Room
    private val database = LibraryDatabase.get(context)

    // Cliente HTTP
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    // Cliente para Gutendex
    private val gutendexClient = client.newBuilder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header(
                    "User-Agent",
                    "LibrisAndroid/1.2 (Android; EducationalReader)"
                )
                .build()
            chain.proceed(request)
        }
        .build()

    // Configuracion general de Retrofit
    private fun retrofit(
        baseUrl: String,
        httpClient: OkHttpClient = client
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // API 1: Google Books
    private val googleApi = retrofit(
        "https://www.googleapis.com/books/v1/"
    ).create(GoogleBooksApi::class.java)

    // API 2: Open Library
    private val openLibraryApi = retrofit(
        "https://openlibrary.org/"
    ).create(OpenLibraryApi::class.java)

    // SQLite Cloud - Edge Functions SQL
    val cloudApi = retrofit(
        "https://cljpl9qcdk.g4.gateway.sqlite.cloud/"
    ).create(LibrisCloudApi::class.java)

    // Repositorio de favoritos local y nube
    val favoritesRepository = FavoritesRepository(
        database.favoriteDao(),
        cloudApi
    )

    // Repositorio de libros
    val bookRepository = BookRepository(
        googleApi,
        openLibraryApi,
        database.bookCacheDao(),
        BuildConfig.GOOGLE_BOOKS_API_KEY
    )

    // Repositorio de autores
    val authorRepository = AuthorRepository(
        openLibraryApi
    )

    // API 3: Gutendex
    val gutenbergRepository = GutenbergRepository(
        retrofit(
            "https://gutendex.com/",
            gutendexClient
        ).create(GutendexApi::class.java)
    )
}

// Factory para los ViewModels
class LibraryViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T = when {

        modelClass.isAssignableFrom(BooksViewModel::class.java) ->
            BooksViewModel(
                container.bookRepository,
                container.favoritesRepository
            ) as T

        modelClass.isAssignableFrom(FavoritesViewModel::class.java) ->
            FavoritesViewModel(
                container.favoritesRepository
            ) as T

        modelClass.isAssignableFrom(AuthorViewModel::class.java) ->
            AuthorViewModel(
                container.authorRepository
            ) as T

        modelClass.isAssignableFrom(ReadingViewModel::class.java) ->
            ReadingViewModel(
                container.gutenbergRepository
            ) as T

        else -> throw IllegalArgumentException(
            "ViewModel desconocido: ${modelClass.name}"
        )
    }
}
