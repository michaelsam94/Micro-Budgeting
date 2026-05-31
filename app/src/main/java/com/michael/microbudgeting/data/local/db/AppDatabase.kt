package com.michael.microbudgeting.data.local.db

import android.content.Context
import android.os.Build
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.michael.microbudgeting.data.local.db.dao.BudgetDao
import com.michael.microbudgeting.data.local.db.dao.CategoryDao
import com.michael.microbudgeting.data.local.db.dao.TransactionDao
import com.michael.microbudgeting.data.local.db.entity.BudgetEntity
import com.michael.microbudgeting.data.local.db.entity.CategoryEntity
import com.michael.microbudgeting.data.local.db.entity.TransactionEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.util.UUID

@Database(
    entities = [TransactionEntity::class, BudgetEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance.db"
                )
                    .fallbackToDestructiveMigration()

                if (Build.FINGERPRINT == "robolectric") {
                    builder.openHelperFactory(FrameworkSQLiteOpenHelperFactory())
                } else {
                    System.loadLibrary("sqlcipher")
                    val passphrase = getOrGeneratePassphrase(context)
                    builder.openHelperFactory(SupportOpenHelperFactory(passphrase))
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        private fun getOrGeneratePassphrase(context: Context): ByteArray {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                val prefs = EncryptedSharedPreferences.create(
                    context,
                    "db_key_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                var key = prefs.getString("db_passphrase", null)
                if (key == null) {
                    key = UUID.randomUUID().toString()
                    prefs.edit().putString("db_passphrase", key).apply()
                }
                key.toByteArray(Charsets.UTF_8)
            } catch (e: Exception) {
                // Fallback for emulator environments without standard KeyStore or general errors
                // Ensure the database still opens but safely fallback to a deterministic key
                "offline-privacy-guardian-fallback-key-2026".toByteArray(Charsets.UTF_8)
            }
        }
    }
}
