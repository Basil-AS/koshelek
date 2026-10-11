package ru.basilas.koshelek.domain.model

import java.time.Instant

enum class CodeFormat {
    QR,
    CODE_128,
    EAN_13,
    PDF_417,
    UNKNOWN,
}

/** A code that can be rendered offline until its optional expiry moment. */
data class CardCodeSnapshot(
    val payload: String,
    val format: CodeFormat,
    val obtainedAt: Instant,
    val expiresAt: Instant? = null,
) {
    init {
        require(payload.isNotBlank()) { "Code payload must not be blank" }
        require(expiresAt == null || expiresAt.isAfter(obtainedAt)) {
            "Code expiry must be after the time it was obtained"
        }
    }

    val isDynamic: Boolean
        get() = expiresAt != null
}
