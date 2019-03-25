package org.sil.storyproducer.test.model

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.sil.storyproducer.model.KeytermCsvReader
import java.io.InputStreamReader

@RunWith(RobolectricTestRunner::class)
class TestKeytermCsvReader {
   @Test
    fun readAll_When_condition_Should_result() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("keyterms.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        
        val keyterms = keytermCsvReader.readAll()
        
        //Assert.assertEquals(expectedPhrases, actualPhrases)
    }
}