package ru.basilas.koshelek.data.security

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts an export payload without a server-side key.
 *
 * The password is never persisted by this class. Callers should keep it only for
 * the duration of an import/export operation and clear the CharArray afterwards.
 */
class ArchiveCrypto(
    private val random: SecureRandom = SecureRandom(),
) {
    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty()) { "Archive password must not be empty" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val key = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(headerAad())
            val ciphertext = cipher.doFinal(plaintext)
            ByteBuffer.allocate(HEADER_BYTES + salt.size + iv.size + ciphertext.size)
                .put(MAGIC)
                .put(VERSION)
                .putInt(PBKDF2_ITERATIONS)
                .put(salt.size.toByte())
                .put(iv.size.toByte())
                .put(salt)
                .put(iv)
                .put(ciphertext)
                .array()
        } catch (error: GeneralSecurityException) {
            throw ArchiveCryptoException("Unable to encrypt archive", error)
        }
    }

    fun decrypt(envelope: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty()) { "Archive password must not be empty" }
        val parsed = parse(envelope)
        val key = deriveKey(password, parsed.salt, parsed.iterations)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, parsed.iv))
            cipher.updateAAD(headerAad())
            cipher.doFinal(parsed.ciphertext)
        } catch (error: GeneralSecurityException) {
            throw ArchiveCryptoException("Unable to decrypt archive", error)
        }
    }

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = PBKDF2_ITERATIONS,
    ): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES").also { bytes.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }

    private fun parse(envelope: ByteArray): ParsedEnvelope {
        try {
            require(envelope.size >= HEADER_BYTES) { "Archive is too short" }
            val buffer = ByteBuffer.wrap(envelope)
            val magic = ByteArray(MAGIC.size).also(buffer::get)
            require(magic.contentEquals(MAGIC)) { "Unsupported archive format" }
            require(buffer.get() == VERSION) { "Unsupported archive version" }
            val iterations = buffer.int
            require(iterations >= MIN_PBKDF2_ITERATIONS) { "Archive KDF parameters are too weak" }
            val saltSize = buffer.get().toInt() and 0xff
            val ivSize = buffer.get().toInt() and 0xff
            require(saltSize == SALT_BYTES && ivSize == IV_BYTES) { "Invalid archive parameters" }
            val salt = ByteArray(saltSize).also(buffer::get)
            val iv = ByteArray(ivSize).also(buffer::get)
            require(buffer.hasRemaining()) { "Archive payload is empty" }
            return ParsedEnvelope(iterations, salt, iv, ByteArray(buffer.remaining()).also(buffer::get))
        } catch (error: RuntimeException) {
            throw ArchiveCryptoException("Invalid archive", error)
        }
    }

    private fun headerAad(): ByteArray = MAGIC + byteArrayOf(VERSION)

    private data class ParsedEnvelope(
        val iterations: Int,
        val salt: ByteArray,
        val iv: ByteArray,
        val ciphertext: ByteArray,
    )

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KDF = "PBKDF2WithHmacSHA256"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val SALT_BYTES = 16
        const val IV_BYTES = 12
        const val PBKDF2_ITERATIONS = 210_000
        const val MIN_PBKDF2_ITERATIONS = 100_000
        val MAGIC = byteArrayOf('K'.code.toByte(), 'W'.code.toByte(), 'A'.code.toByte(), '1'.code.toByte())
        const val VERSION: Byte = 1
        const val HEADER_BYTES = 4 + 1 + 4 + 1 + 1
    }
}

class ArchiveCryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)

