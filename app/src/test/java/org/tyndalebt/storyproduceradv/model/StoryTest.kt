package org.tyndalebt.storyproduceradv.model

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.ArrayList

@RunWith(MockitoJUnitRunner::class)
class StoryTest {
    @Test
    fun testShortTitle() {
        val storyWithoutNumbers = Story("Creation", ArrayList())
        assertEquals("Expected short title to return full title", "Creation", storyWithoutNumbers.shortTitle)

        val storyWithNumbers = Story("03 Fall", ArrayList())
        assertEquals("Expected short title to return title without numbers", "Fall", storyWithNumbers.shortTitle)
    }
}
