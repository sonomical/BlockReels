package com.blockreels.app.data

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local cryptographic PIN hashing using salted PBKDF2WithHmacSHA256.
 * Never stores plaintext PINs.
 */
object CryptoPinHelper {
    private const val ITERATIONS = 12_000
    private const val KEY_LENGTH_BITS = 256

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, saltHex: String): String {
        return try {
            val spec = PBEKeySpec(pin.toCharArray(), hexToBytes(saltHex), ITERATIONS, KEY_LENGTH_BITS)
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            skf.generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(saltHex.toByteArray(Charsets.UTF_8))
            digest.digest(pin.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        }
    }

    fun verifyPin(candidatePin: String, expectedHash: String, saltHex: String): Boolean {
        if (expectedHash.isBlank() || saltHex.isBlank()) return false
        val actual = hashPin(candidatePin, saltHex)
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = if (hex.length % 2 == 0) hex else "0$hex"
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
