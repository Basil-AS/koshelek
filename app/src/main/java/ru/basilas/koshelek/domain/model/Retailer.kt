package ru.basilas.koshelek.domain.model

/** Supported loyalty providers. The enum is deliberately open for future adapter additions. */
enum class Retailer(
    val stableId: String,
    val displayName: String,
) {
    MAGNIT("magnit", "Магнит"),
    PEREKRESTOK("perekrestok", "Перекрёсток"),
    PYATEROCHKA("pyaterochka", "Пятёрочка"),
    ULYBKA_RADUGI("ulybka-radugi", "Улыбка радуги"),
    MAGNOLIA("magnolia", "Магнолия"),
}
