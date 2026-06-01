package com.novamesh.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.novamesh.data.local.dao.ChatDao
import com.novamesh.data.local.dao.MessageDao
import com.novamesh.data.local.dao.UserDao
import com.novamesh.data.local.entity.ChatEntity
import com.novamesh.data.local.entity.MessageEntity
import com.novamesh.data.local.entity.UserEntity
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Room database for NovaMesh.
 *
 * Uses standard (non-encrypted) SQLite for now.
 * In production, use SQLCipher or Android Keystore for at-rest encryption.
 *
 * Entities: [MessageEntity], [ChatEntity], [UserEntity]
 */
@Database(
    entities = [MessageEntity::class, ChatEntity::class, UserEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun chatDao(): ChatDao
    abstract fun userDao(): UserDao

    companion object {
        private const val DB_NAME = "novamesh.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get or create the singleton [AppDatabase] instance.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Build the Room database.
         */
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DB_NAME,
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * Close and destroy the singleton instance.
         */
        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }

        /**
         * Enable WAL mode for better concurrent read performance.
         */
        fun enableWAL(database: SupportSQLiteDatabase) {
            database.execSQL("PRAGMA journal_mode=WAL")
            database.execSQL("PRAGMA foreign_keys=ON")
        }
    }
}
