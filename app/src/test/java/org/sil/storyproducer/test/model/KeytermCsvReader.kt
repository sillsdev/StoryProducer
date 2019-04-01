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

    @Test
    fun readAll_When_MultipleRelatedTerms_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommas() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTerms.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingWhitespaceInTerm_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithExtraWhitespaceRemoved() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTermsWithLeadingWhitespaceInTerm.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithTrailingWhitespaceInTerm_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithExtraWhitespaceRemoved() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTermsWithTrailingWhitespaceInTerm.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingAndTrailingWhitespaceInTerm_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithExtraWhitespaceRemoved() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTermsWithLeadingAndTrailingWhitespaceInTerm.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingWhitespaceInField_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithExtraWhitespaceRemoved() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTermsWithLeadingWhitespaceInField.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithTrailingWhitespaceInField_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithExtraWhitespaceRemoved() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTermsWithTrailingWhitespaceInField.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingAndTrailingWhitespaceInField_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithExtraWhitespaceRemoved() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTermsWithLeadingAndTrailingWhitespaceInField.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingComma_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithEmptyRelatedTermsRemoved() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTermsWithLeadingComma.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithTrailingComma_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithEmptyRelatedTermsRemoved() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTermsWithTrailingComma.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingAndTrailingComma_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithEmptyRelatedTermsRemoved() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTermsWithLeadingAndTrailingComma.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingCommas_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithEmptyRelatedTermsRemoved() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTermsWithLeadingCommas.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithTrailingCommas_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithEmptyRelatedTermsRemoved() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTermsWithTrailingCommas.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingAndTrailingCommas_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithEmptyRelatedTermsRemoved() {
        val inputStream = this.javaClass.classLoader?.getResourceAsStream("KeytermCsvReader/MultipleRelatedTermsWithLeadingAndTrailingCommas.csv")
        val streamReader = InputStreamReader(inputStream)
        val keytermCsvReader = KeytermCsvReader(streamReader)
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }
}