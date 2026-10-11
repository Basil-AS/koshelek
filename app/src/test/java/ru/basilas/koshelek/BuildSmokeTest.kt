package ru.basilas.koshelek

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildSmokeTest {
    @Test
    fun applicationMetadataIsConfigured() {
        assertEquals("ru.basilas.koshelek", BuildConfig.APPLICATION_ID)
        assertTrue(BuildConfig.VERSION_NAME.isNotBlank())
    }
}
