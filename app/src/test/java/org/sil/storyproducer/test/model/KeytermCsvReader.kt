package org.sil.storyproducer.test.model

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.sil.storyproducer.model.Keyterm
import org.sil.storyproducer.model.KeytermCsvReader
import java.io.InputStreamReader

@RunWith(RobolectricTestRunner::class)
class TestKeytermCsvReader {
    @Test
    fun readAll_When_AllFieldsFilled_Should_ReturnListWithOneKeytermWithAllThoseFields() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/AllFieldsFilled.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("disciples"), listOf("student"), "Some notes.", listOf("apostle")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_TermFieldEmptyAndOtherFieldsFilled_Should_ReturnEmptyList() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/TermFieldEmptyAndOtherFieldsFilled.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertTrue(actualKeyterms.isEmpty())
    }

    @Test
    fun readAll_When_OtherFormsFieldEmptyAndOtherFieldsFilled_Should_ReturnListWithOneKeytermWithAllThoseFields() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/OtherFormsFieldEmptyAndOtherFieldsFilled.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("student"), "Some notes.", listOf("apostle")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_AlternateRenderingsFieldEmptyAndOtherFieldsFilled_Should_ReturnListWithOneKeytermWithAllThoseFields() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/AlternateRenderingsFieldEmptyAndOtherFieldsFilled.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("disciples"), listOf(), "Some notes.", listOf("apostle")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_NotesFieldEmptyAndOtherFieldsFilled_Should_ReturnListWithOneKeytermWithAllThoseFields() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/NotesFieldEmptyAndOtherFieldsFilled.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("disciples"), listOf("student"), "", listOf("apostle")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }


    @Test
    fun readAll_When_RelatedTermsFieldEmptyAndOtherFieldsFilled_Should_ReturnListWithOneKeytermWithAllThoseFields() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/RelatedTermsFieldEmptyAndOtherFieldsFilled.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("disciples"), listOf("student"), "Some notes.", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }
}