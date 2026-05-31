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
import net.zetetic.database.sqlcipher.SupportFactory
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Encrypted Room database using SQLCipher for at-rest encryption.
 *
 * The database uses a passphrase-derived key (PBKDF2 with 100 000 iterations)
 * to initialize SQLCipher's [SupportFactory]. The passphrase should be derived
 * from the user's login password or a device-stored key.
 *
 * Entities: [MessageEntity], [ChatEntity], [UserEntity]
 *
 * Usage:
 * ```
 * val passphrase = AppDatabase.derivePassphrase(userPassword, userId)
 * val db = AppDatabase.getInstance(context, passphrase)
 * ```
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
        private const val DB_NAME = "novamesh_encrypted.db"
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 32

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get or create the singleton [AppDatabase] instance.
         *
         * @param context Application context
         * @param passphrase Byte array passphrase for SQLCipher (32+ bytes recommended)
         * @return The singleton database instance
         */
        fun getInstance(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, passphrase).also { INSTANCE = it }
            }
        }

        /**
         * Build the Room database with SQLCipher encryption.
         *
         * Uses [SupportFactory] with the provided passphrase to encrypt
         * the SQLite database file. [fallbackToDestructiveMigration] is
         * enabled for development; replace with proper migrations in production.
         */
        private fun buildDatabase(context: Context, passphrase: ByteArray): AppDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DB_NAME,
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * Derive a 256-bit encryption key from a password using PBKDF2-HMAC-SHA256.
         *
         * The salt is generated deterministically from [saltInput] so the same
         * password always produces the same key for the same user, without
         * needing to store a salt separately.
         *
         * @param password The user's password (or a strong passphrase)
         * @param saltInput A unique, stable string (e.g. user ID + app-specific pepper)
         * @return 256-bit derived key as a ByteArray
         */
        fun derivePassphrase(password: String, saltInput: String): ByteArray {
            val salt = saltInput.toByteArray(Charsets.UTF_8).copyOf(SALT_LENGTH)
            val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return factory.generateSecret(spec).encoded
        }

        /**
         * Generate a random 256-bit passphrase (for first-time setup or key rotation).
         */
        fun generateRandomPassphrase(): ByteArray {
            val random = SecureRandom()
            val key = ByteArray(32)
            random.nextBytes(key)
            return key
        }

        /**
         * Close and destroy the singleton instance.
         * Call this during logout or when clearing all local data.
         */
        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }

        /**
         * Enable WAL mode for better concurrent read performance.
         * Call this after [getInstance] if needed.
         */
        fun enableWAL(database: SupportSQLiteDatabase) {
            database.execSQL("PRAGMA journal_mode=WAL")
            database.execSQL("PRAGMA foreign_keys=ON")
        }
    }
}
