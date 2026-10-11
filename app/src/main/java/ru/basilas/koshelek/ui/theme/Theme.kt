package ru.basilas.koshelek.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KoshelekColors = lightColorScheme()

@Composable
fun KoshelekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KoshelekColors,
        content = content,
    )
}
