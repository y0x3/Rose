package com.tiempo.app.data.local

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryPinStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private companion object {
        const val KEY_SALT = "salt"
        const val KEY_HASH = "hash"
        const val KEY_R_SALT = "recovery_salt"
        const val KEY_R_HASH = "recovery_hash"
        // Sin 0/O ni 1/I para que no se confundan al copiarlo a mano
        const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val CODE_LENGTH = 12
    }

    private val prefs = context.getSharedPreferences("history_lock", Context.MODE_PRIVATE)
    private val random = SecureRandom()

    fun hasPin(): Boolean = prefs.contains(KEY_HASH)

    /** Guarda el PIN y genera un código de recuperación nuevo. Devuelve el código (solo se ve ahora). */
    fun setPin(pin: String): String {
        val code = newRecoveryCode()
        val salt = randomBytes(16)
        val recoverySalt = randomBytes(16)
        prefs.edit()
            .putString(KEY_SALT, encode(salt))
            .putString(KEY_HASH, encode(hash(salt, pin)))
            .putString(KEY_R_SALT, encode(recoverySalt))
            .putString(KEY_R_HASH, encode(hash(recoverySalt, normalize(code))))
            .apply()
        return format(code)
    }

    fun check(pin: String): Boolean = matches(KEY_SALT, KEY_HASH, pin)

    fun checkRecoveryCode(input: String): Boolean =
        matches(KEY_R_SALT, KEY_R_HASH, normalize(input))

    fun clear() {
        prefs.edit()
            .remove(KEY_SALT).remove(KEY_HASH)
            .remove(KEY_R_SALT).remove(KEY_R_HASH)
            .apply()
    }

    private fun matches(saltKey: String, hashKey: String, value: String): Boolean {
        val salt = prefs.getString(saltKey, null)?.let(::decode) ?: return false
        val stored = prefs.getString(hashKey, null)?.let(::decode) ?: return false
        return MessageDigest.isEqual(stored, hash(salt, value))
    }

    private fun newRecoveryCode(): String =
        buildString { repeat(CODE_LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    private fun format(code: String): String = code.chunked(4).joinToString("-")

    private fun normalize(input: String): String =
        input.uppercase().filter { it.isLetterOrDigit() }

    private fun randomBytes(size: Int) = ByteArray(size).also { random.nextBytes(it) }

    private fun hash(salt: ByteArray, value: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(salt + value.toByteArray())

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(text: String): ByteArray = Base64.decode(text, Base64.NO_WRAP)
}