package ru.basilas.koshelek.domain.model

import java.time.Instant
import java.util.UUID

data class LoyaltyCard(
    val id: String = UUID.randomUUID().toString(),
    val retailer: Retailer,
    val label: String = retailer.displayName,
    val code: CardCodeSnapshot,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isFavorite: Boolean = false,
) {
    init {
        require(label.isNotBlank()) { "Card label must not be blank" }
        require(!updatedAt.isBefore(createdAt)) { "Card update time must not precede creation time" }
    }
}
