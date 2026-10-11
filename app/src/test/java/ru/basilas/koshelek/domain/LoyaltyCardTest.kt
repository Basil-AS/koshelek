package ru.basilas.koshelek.domain

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ru.basilas.koshelek.domain.model.CardCodeSnapshot
import ru.basilas.koshelek.domain.model.CodeFormat
import ru.basilas.koshelek.domain.model.LoyaltyCard
import ru.basilas.koshelek.domain.model.Retailer
import ru.basilas.koshelek.domain.refresh.CardRefreshPolicy

class LoyaltyCardTest {
    private val obtainedAt = Instant.parse("2026-10-12T12:00:00Z")

    @Test
    fun storesMerchantIdentifier() {
        val card = card(Retailer.MAGNIT, staticCode("4601234567890", CodeFormat.EAN_13))

        assertEquals("magnit", card.retailer.stableId)
    }

    @Test
    fun supportsStaticBarcodeAndQrCodes() {
        val barcode = card(Retailer.PEREKRESTOK, staticCode("4601234567890", CodeFormat.EAN_13))
        val qr = card(Retailer.PYATEROCHKA, staticCode("https://example.test/card/123", CodeFormat.QR))

        assertEquals("4601234567890", barcode.code.payload)
        assertEquals(CodeFormat.EAN_13, barcode.code.format)
        assertEquals("https://example.test/card/123", qr.code.payload)
        assertEquals(CodeFormat.QR, qr.code.format)
        assertFalse(barcode.code.isDynamic)
        assertFalse(qr.code.isDynamic)
        assertTrue(CardRefreshPolicy().canDisplay(barcode.code, now = obtainedAt))
        assertTrue(CardRefreshPolicy().canDisplay(qr.code, now = obtainedAt))
    }

    @Test
    fun dynamicCodeContainsExpiryTimestamp() {
        val expiresAt = Instant.parse("2026-10-12T12:05:00Z")
        val code = CardCodeSnapshot("dynamic-qr-payload", CodeFormat.QR, obtainedAt, expiresAt)

        assertEquals(expiresAt, code.expiresAt)
        assertTrue(code.isDynamic)
    }

    @Test
    fun dynamicCodeIsNotStaleBeforeExpiry() {
        val expiresAt = Instant.parse("2026-10-12T12:05:00Z")
        val code = CardCodeSnapshot("payload", CodeFormat.QR, obtainedAt, expiresAt)

        assertTrue(CardRefreshPolicy().canDisplay(code, now = expiresAt.minusSeconds(1)))
    }

    @Test
    fun dynamicCodeIsStaleAtAndAfterExpiry() {
        val expiresAt = Instant.parse("2026-10-12T12:05:00Z")
        val code = CardCodeSnapshot("payload", CodeFormat.QR, obtainedAt, expiresAt)

        assertFalse(CardRefreshPolicy().canDisplay(code, now = expiresAt))
        assertFalse(CardRefreshPolicy().canDisplay(code, now = expiresAt.plusSeconds(1)))
    }

    private fun card(retailer: Retailer, code: CardCodeSnapshot) = LoyaltyCard(
        retailer = retailer,
        code = code,
        createdAt = obtainedAt,
        updatedAt = obtainedAt,
    )

    private fun staticCode(payload: String, format: CodeFormat) =
        CardCodeSnapshot(payload, format, obtainedAt)
}
