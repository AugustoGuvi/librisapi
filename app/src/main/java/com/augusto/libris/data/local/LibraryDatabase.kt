package com.augusto.libris.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 favoritos, v2 caché, v3 notas, v4 fuente de búsqueda y paginación. */
@Database(
    entities = [FavoriteEntity::class, BookCacheEntity::class, SearchCacheEntity::class],
    version = 4,
    exportSchema = false
)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun bookCacheDao(): BookCacheDao

    companion object {
        @Volatile private var instance: LibraryDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `book_cache` (`queryKey` TEXT NOT NULL, `bookId` TEXT NOT NULL, `position` INTEGER NOT NULL, `bookJson` TEXT NOT NULL, PRIMARY KEY(`queryKey`, `bookId`))""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_cache_queryKey` ON `book_cache` (`queryKey`)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `search_cache` (`queryKey` TEXT NOT NULL, `queryText` TEXT NOT NULL, `totalItems` INTEGER NOT NULL, `savedAt` INTEGER NOT NULL, PRIMARY KEY(`queryKey`))""")
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `favorites` ADD COLUMN `note` TEXT")
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `search_cache` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'GOOGLE_BOOKS'")
                db.execSQL("ALTER TABLE `search_cache` ADD COLUMN `nextOffset` INTEGER NOT NULL DEFAULT 0")
                // Recupera el índice de las búsquedas ya guardadas sin borrar registros.
                db.execSQL("""UPDATE `search_cache` SET `nextOffset` =
                    (SELECT COUNT(*) FROM `book_cache` WHERE `book_cache`.`queryKey` = `search_cache`.`queryKey`)""")
            }
        }

        fun get(context: Context): LibraryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, LibraryDatabase::class.java, "libris.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
