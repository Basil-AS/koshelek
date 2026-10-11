package ru.basilas.koshelek.data.security

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ArchiveCryptoTest {
    private val crypto = ArchiveCrypto()
    private val password = "correct horse battery staple".toCharArray()

    @Test
    fun encryptDecryptRoundTrip() {
        val plaintext = "архив карт Koshelek".encodeToByteArray()

        val envelope = crypto.encrypt(plaintext, password)
        val decrypted = crypto.decrypt(envelope, password)

        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun samePlaintextProducesDifferentEnvelopes() {
        val plaintext = "same payload".encodeToByteArray()

        val first = crypto.encrypt(plaintext, password)
        val second = crypto.encrypt(plaintext, password)

        assertNotEquals(first.toList(), second.toList())
    }

    @Test
    fun wrongPasswordIsRejected() {
        val envelope = crypto.encrypt("private data".encodeToByteArray(), password)

        assertFailsWith<ArchiveCryptoException> {
            crypto.decrypt(envelope, "wrong password".toCharArray())
        }
    }

    @Test
    fun corruptedEnvelopeIsRejected() {
        val envelope = crypto.encrypt("private data".encodeToByteArray(), password)
        val corrupted = envelope.copyOf().also {
            it[it.lastIndex] = (it.last().toInt() xor 0x01).toByte()
        }

        assertFailsWith<ArchiveCryptoException> {
            crypto.decrypt(corrupted, password)
        }
    }
}
