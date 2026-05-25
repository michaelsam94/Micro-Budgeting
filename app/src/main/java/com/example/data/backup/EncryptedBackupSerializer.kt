package com.example.data.backup

import android.content.Context
import com.example.data.local.db.entity.BudgetEntity
import com.example.data.local.db.entity.CategoryEntity
import com.example.data.local.db.entity.TransactionEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@JsonClass(generateAdapter = true)
data class BackupCategory(
    val id: Long,
    val name: String,
    val iconName: String,
    val colorHex: String
)

@JsonClass(generateAdapter = true)
data class BackupTransaction(
    val id: Long,
    val amount: Double,
    val categoryId: Long,
    val note: String,
    val timestamp: Long,
    val source: String,
    val rawSms: String?
)

@JsonClass(generateAdapter = true)
data class BackupBudget(
    val id: Long,
    val categoryId: Long,
    val limitAmount: Double,
    val month: String
)

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val categories: List<BackupCategory>,
    val transactions: List<BackupTransaction>,
    val budgets: List<BackupBudget>
)

class EncryptedBackupSerializer {

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(BackupPayload::class.java)

    private val magicBytes = byteArrayOf(0x46, 0x42, 0x41, 0x4B) // "FBAK"

    fun exportEncrypted(payload: BackupPayload, password: String): ByteArray {
        val json = adapter.toJson(payload)
        val jsonBytes = json.toByteArray(StandardCharsets.UTF_8)

        // Generate 16 bytes salt
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        // Derive key from password and salt
        val key = deriveKey(password, salt)

        // Generate 12 bytes IV (standard GCM IV is 12 bytes)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(jsonBytes)

        // Compound format: [4-byte magic][16-byte salt][12-byte iv][ciphertext]
        val totalSize = magicBytes.size + salt.size + iv.size + ciphertext.size
        val outputBytes = ByteArray(totalSize)

        System.arraycopy(magicBytes, 0, outputBytes, 0, magicBytes.size)
        System.arraycopy(salt, 0, outputBytes, magicBytes.size, salt.size)
        System.arraycopy(iv, 0, outputBytes, magicBytes.size + salt.size, iv.size)
        System.arraycopy(ciphertext, 0, outputBytes, magicBytes.size + salt.size + iv.size, ciphertext.size)

        return outputBytes
    }

    fun importEncrypted(bytes: ByteArray, password: String): BackupPayload {
        require(bytes.size > 32) { "Invalid backup file size." }
        
        // Match magic bytes
        for (i in magicBytes.indices) {
            if (bytes[i] != magicBytes[i]) {
                throw SecurityException("Invalid backup file magic header.")
            }
        }

        val salt = ByteArray(16)
        val iv = ByteArray(12)
        val ciphertext = ByteArray(bytes.size - 32)

        System.arraycopy(bytes, 4, salt, 0, 16)
        System.arraycopy(bytes, 20, iv, 0, 12)
        System.arraycopy(bytes, 32, ciphertext, 0, ciphertext.size)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        
        val decryptedBytes = cipher.doFinal(ciphertext)
        val json = String(decryptedBytes, StandardCharsets.UTF_8)
        
        return adapter.fromJson(json) ?: throw IllegalStateException("Failed to parse backup payload json.")
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        // PBKDF2 with standard HMAC SHA256
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        // 10,000 runs is fast and perfectly secure for on-device operations
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }
}
