package com.augusto.libris.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Pruebas instrumentadas: ejecutar con emulador/telefono desde Android Studio. */
@RunWith(AndroidJUnit4::class)
class RoomPersistenceTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val fileName = "libris_test.db"

    private fun database(): LibraryDatabase = Room.databaseBuilder(
        context, LibraryDatabase::class.java, fileName
    ).addMigrations(LibraryDatabase.MIGRATION_1_2, LibraryDatabase.MIGRATION_2_3, LibraryDatabase.MIGRATION_3_4).build()

    @After fun clean() { context.deleteDatabase(fileName) }

    @Test fun favoritesAndNotesSurviveRestart() = runBlocking {
        context.deleteDatabase(fileName)
        val first = database()
        first.favoriteDao().insert(FavoriteEntity(
            bookId = "google-123", title = "Libro de prueba", authorLabel = "Autor",
            coverUrl = null, bookJson = "{}"
        ))
        first.favoriteDao().updateNote("google-123", "Leer el capitulo 3")
        first.close()

        val reopened = database()
        val data = reopened.favoriteDao().observeRecent().first()
        assertEquals(1, data.size)
        assertEquals("Leer el capitulo 3", data.first().note)
        reopened.close()
    }

    @Test fun openLibraryCacheKeepsSourceAndNextOffsetAfterRestart() = runBlocking {
        context.deleteDatabase(fileName)
        val first = database()
        first.bookCacheDao().replaceResults(
            SearchCacheEntity(
                queryKey = "dracula", queryText = "Dracula", totalItems = 60,
                source = "OPEN_LIBRARY", nextOffset = 20
            ),
            listOf(BookCacheEntity("dracula", "openlibrary:/works/OL1W", 0, "{}"))
        )
        first.close()

        val reopened = database()
        val cached = reopened.bookCacheDao().getSearch("dracula")!!
        assertEquals("OPEN_LIBRARY", cached.source)
        assertEquals(20, cached.nextOffset)
        assertEquals(60, cached.totalItems)
        reopened.close()
    }

    @Test fun cacheReplacesOneQueryButKeepsOtherQueriesAndFavorites() = runBlocking {
        context.deleteDatabase(fileName)
        val db = database()
        db.favoriteDao().insert(FavoriteEntity(
            bookId = "fav", title = "Conservar", authorLabel = "", coverUrl = null, bookJson = "{}"
        ))
        val dao = db.bookCacheDao()
        dao.replaceResults(SearchCacheEntity("don quijote", "Don Quijote", 1),
            listOf(BookCacheEntity("don quijote", "one", 0, "{}")))
        dao.replaceResults(SearchCacheEntity("dracula", "Dracula", 1),
            listOf(BookCacheEntity("dracula", "two", 0, "{}")))
        dao.replaceResults(SearchCacheEntity("don quijote", "Don Quijote", 1),
            listOf(BookCacheEntity("don quijote", "three", 0, "{}")))
        assertEquals("three", dao.observeBooks("don quijote").first().single().bookId)
        assertEquals("two", dao.observeBooks("dracula").first().single().bookId)
        assertTrue(db.favoriteDao().exists("fav"))
        db.close()
    }
}
