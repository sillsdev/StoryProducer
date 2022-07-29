package org.tyndalebt.spadv.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StoryIOTest {

    // For the purposes of parsing the bloom files, a file is considered a zipped file if the
    // extension is one of these 3:
    // - zip
    // - bloom
    // - bloomd
    @Test
    fun testIsZipped() {
        // not zipped
        assertFalse(isZipped(null))
        assertFalse(isZipped(""))
        assertFalse(isZipped("225 Walk on water"))
        assertFalse(isZipped("225 Walk on water."))
        assertFalse(isZipped("225 Walk on water.txt"))
        assertFalse(isZipped("225.Walk.on.water.txt"))

        // zipped
        assertTrue(isZipped("225.Walk.on.water.zip"))
        assertTrue(isZipped("225 Walk on water.zip"))
        assertTrue(isZipped("225 Walk on water.bloom"))
        assertTrue(isZipped("225 Walk on water.bloomd"))
    }

}