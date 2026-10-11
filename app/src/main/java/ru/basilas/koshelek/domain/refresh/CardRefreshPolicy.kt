package ru.basilas.koshelek.domain.refresh

import java.time.Instant
import ru.basilas.koshelek.domain.model.CardCodeSnapshot

/** Decides whether a cached code is safe to show at a point in time. */
class CardRefreshPolicy {
    fun canDisplay(snapshot: CardCodeSnapshot, now: Instant): Boolean =
        snapshot.expiresAt?.let { now.isBefore(it) } ?: true
}
