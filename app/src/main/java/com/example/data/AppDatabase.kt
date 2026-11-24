package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [VideoEntity::class, PdfEntity::class, PdfAnnotationEntity::class, VideoShotEntity::class, VideoNoteEntity::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun pdfDao(): PdfDao
    abstract fun pdfAnnotationDao(): PdfAnnotationDao
    abstract fun videoShotDao(): VideoShotDao
    abstract fun videoNoteDao(): VideoNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE videos ADD COLUMN duration INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE videos ADD COLUMN lastPlayedPosition INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aether_video_database"
                )
                .addMigrations(MIGRATION_10_11)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
