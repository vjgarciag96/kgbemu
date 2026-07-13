package com.vicgarci.kgbem.test

import kotlin.test.Test
import kotlin.test.assertTrue

class TestResourcesTest {
    @Test
    fun dmgAcid2_isPresent() {
        val bytes = loadTestResource("dmg-acid2.gb")
        assertTrue(bytes.size > 1000, "dmg-acid2.gb should be at least 1 KB, got ${bytes.size} bytes")
    }
}
