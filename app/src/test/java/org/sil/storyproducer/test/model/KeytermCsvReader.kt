package org.sil.storyproducer.test.model

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.sil.storyproducer.model.Keyterm
import org.sil.storyproducer.model.KeytermCsvReader
import java.io.InputStreamReader

@RunWith(RobolectricTestRunner::class)
class TestKeytermCsvReader {
    @Rule
    @JvmField
    var expectedException: ExpectedException = ExpectedException.none()

    @Test
    fun readAll_When_AllFieldsFilled_Should_ReturnListWithOneKeytermWithAllThoseFields() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/AllFieldsFilled.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("disciples"), listOf("student"), "Some notes.", listOf("apostle")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_TermFieldEmptyAndOtherFieldsFilled_Should_ReturnEmptyList() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/TermFieldEmptyAndOtherFieldsFilled.csv")

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertTrue(actualKeyterms.isEmpty())
    }

    @Test
    fun readAll_When_OtherFormsFieldEmptyAndOtherFieldsFilled_Should_ReturnListWithOneKeytermWithAllThoseFields() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/OtherFormsFieldEmptyAndOtherFieldsFilled.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("student"), "Some notes.", listOf("apostle")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_AlternateRenderingsFieldEmptyAndOtherFieldsFilled_Should_ReturnListWithOneKeytermWithAllThoseFields() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/AlternateRenderingsFieldEmptyAndOtherFieldsFilled.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("disciples"), listOf(), "Some notes.", listOf("apostle")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_NotesFieldEmptyAndOtherFieldsFilled_Should_ReturnListWithOneKeytermWithAllThoseFields() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/NotesFieldEmptyAndOtherFieldsFilled.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("disciples"), listOf("student"), "", listOf("apostle")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }


    @Test
    fun readAll_When_RelatedTermsFieldEmptyAndOtherFieldsFilled_Should_ReturnListWithOneKeytermWithAllThoseFields() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/RelatedTermsFieldEmptyAndOtherFieldsFilled.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("disciples"), listOf("student"), "Some notes.", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_EmptyCsvFile_Should_ReturnEmptyListOfKeyterms() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/Empty.csv")

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertTrue(actualKeyterms.isEmpty())
    }



    @Test
    fun readAll_When_EmptyRowBeforeFilledRow_Should_ThrowIndexOutOfBoundsException() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/EmptyRowBeforeFilledRow.csv")
        
        expectedException.expect(IndexOutOfBoundsException::class.java)

        keytermCsvReader.readAll()
    }

    @Test
    fun readAll_When_RowWithAllEmptyFieldsBeforeFilledRow_Should_ReturnListWithOneKeyterm() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/RowWithAllEmptyFieldsBeforeFilledRow.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("disciples"), listOf("student"), "Some notes.", listOf("apostle")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_TooFewColumns_Should_ThrowIndexOutOfBoundsException() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/TooFewColumns.csv")

        expectedException.expect(IndexOutOfBoundsException::class.java)

        keytermCsvReader.readAll()
    }

    @Test
    fun readAll_When_TextInOtherColumns_Should_ReturnListWithOneKeytermAndIgnoreOtherColumns() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/TextInOtherColumns.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("disciples"), listOf("student"), "Some notes.", listOf("apostle")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_FiveRowsOfKeyterms_Should_ReturnListWithFiveKeyterms() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/FiveRowsOfKeyterms.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("term1", listOf("disciples"), listOf("student"), "Some notes.", listOf("apostle")))
        expectedKeyterms.add(Keyterm("term2", listOf("disciples"), listOf("student"), "Some notes.", listOf("apostle")))
        expectedKeyterms.add(Keyterm("term3", listOf("disciples"), listOf("student"), "Some notes.", listOf("apostle")))
        expectedKeyterms.add(Keyterm("term4", listOf("disciples"), listOf("student"), "Some notes.", listOf("apostle")))
        expectedKeyterms.add(Keyterm("term5", listOf("disciples"), listOf("student"), "Some notes.", listOf("apostle")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_TwoDuplicateRowsOfKeyterms_Should_ReturnListWithTwoKeyterms() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/TwoDuplicateRowsOfKeyterms.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("disciples"), listOf("student"), "Some notes.", listOf("apostle")))
        expectedKeyterms.add(Keyterm("disciple", listOf("disciples"), listOf("student"), "Some notes.", listOf("apostle")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTerms_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommas() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTerms.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingWhitespaceInTerm_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTermsWithLeadingWhitespaceInTerm.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithTrailingWhitespaceInTerm_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTermsWithTrailingWhitespaceInTerm.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingAndTrailingWhitespaceInTerm_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTermsWithLeadingAndTrailingWhitespaceInTerm.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingWhitespaceInField_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTermsWithLeadingWhitespaceInField.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithTrailingWhitespaceInField_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTermsWithTrailingWhitespaceInField.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingAndTrailingWhitespaceInField_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTermsWithLeadingAndTrailingWhitespaceInField.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingComma_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithEmptyRelatedTermsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTermsWithLeadingComma.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithTrailingComma_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithEmptyRelatedTermsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTermsWithTrailingComma.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingAndTrailingComma_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithEmptyRelatedTermsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTermsWithLeadingAndTrailingComma.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingCommas_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithEmptyRelatedTermsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTermsWithLeadingCommas.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithTrailingCommas_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithEmptyRelatedTermsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTermsWithTrailingCommas.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleRelatedTermsWithLeadingAndTrailingCommas_Should_ReturnListWithOneKeytermWithListOfRelatedTermsSplitOnCommasWithEmptyRelatedTermsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleRelatedTermsWithLeadingAndTrailingCommas.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf("thing", "something else", "completely different yet related thing")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherForms_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommas() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherForms.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else", "completely different yet related thing"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherFormsWithLeadingWhitespaceInTerm_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommasWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherFormsWithLeadingWhitespaceInTerm.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherFormsWithTrailingWhitespaceInTerm_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommasWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherFormsWithTrailingWhitespaceInTerm.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherFormsWithLeadingAndTrailingWhitespaceInTerm_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommasWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherFormsWithLeadingAndTrailingWhitespaceInTerm.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherFormsWithLeadingWhitespaceInField_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommasWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherFormsWithLeadingWhitespaceInField.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherFormsWithTrailingWhitespaceInField_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommasWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherFormsWithTrailingWhitespaceInField.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherFormsWithLeadingAndTrailingWhitespaceInField_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommasWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherFormsWithLeadingAndTrailingWhitespaceInField.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherFormsWithLeadingComma_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommasWithEmptyOtherFormsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherFormsWithLeadingComma.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else", "completely different yet related thing"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherFormsWithTrailingComma_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommasWithEmptyOtherFormsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherFormsWithTrailingComma.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else", "completely different yet related thing"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherFormsWithLeadingAndTrailingComma_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommasWithEmptyOtherFormsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherFormsWithLeadingAndTrailingComma.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else", "completely different yet related thing"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherFormsWithLeadingCommas_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommasWithEmptyOtherFormsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherFormsWithLeadingCommas.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else", "completely different yet related thing"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherFormsWithTrailingCommas_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommasWithEmptyOtherFormsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherFormsWithTrailingCommas.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else", "completely different yet related thing"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleOtherFormsWithLeadingAndTrailingCommas_Should_ReturnListWithOneKeytermWithListOfOtherFormsSplitOnCommasWithEmptyOtherFormsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleOtherFormsWithLeadingAndTrailingCommas.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("thing", "something else", "completely different yet related thing"), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderings_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColons() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderings.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else", "completely different yet related thing"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderingsWithLeadingWhitespaceInTerm_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderingsWithLeadingWhitespaceInTerm.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderingsWithTrailingWhitespaceInTerm_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderingsWithTrailingWhitespaceInTerm.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderingsWithLeadingAndTrailingWhitespaceInTerm_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderingsWithLeadingAndTrailingWhitespaceInTerm.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderingsWithLeadingWhitespaceInField_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderingsWithLeadingWhitespaceInField.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderingsWithTrailingWhitespaceInField_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderingsWithTrailingWhitespaceInField.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderingsWithLeadingAndTrailingWhitespaceInField_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderingsWithLeadingAndTrailingWhitespaceInField.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderingsWithLeadingSemiColon_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithEmptyAlternateRenderingsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderingsWithLeadingSemiColon.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else", "completely different yet related thing"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderingsWithTrailingSemiColon_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithEmptyAlternateRenderingsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderingsWithTrailingSemiColon.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else", "completely different yet related thing"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderingsWithLeadingAndTrailingSemiColon_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithEmptyAlternateRenderingsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderingsWithLeadingAndTrailingSemiColon.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else", "completely different yet related thing"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderingsWithLeadingSemiColons_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithEmptyAlternateRenderingsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderingsWithLeadingSemiColons.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else", "completely different yet related thing"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderingsWithTrailingSemiColons_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithEmptyAlternateRenderingsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderingsWithTrailingSemiColons.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else", "completely different yet related thing"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_MultipleAlternateRenderingsWithLeadingAndTrailingSemiColons_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithEmptyAlternateRenderingsRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/MultipleAlternateRenderingsWithLeadingAndTrailingSemiColons.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("thing", "something else", "completely different yet related thing"), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_OtherFormContainingSemiColon_Should_ReturnListWithOneKeytermWithWholeOtherFormContainingSemiColon() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/OtherFormContainingSemiColon.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf("something; else"), listOf(),"", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_AlternateRenderingContainingComma_Should_ReturnListWithOneKeytermWithWholeAlternateRenderingContainingComma() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/AlternateRenderingContainingComma.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf("something, else"),"", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_RelatedTermContainingSemiColon_Should_ReturnListWithOneKeytermWithWholeRelatedTermContainingSemiColon() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/RelatedTermContainingSemiColon.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(),"", listOf("something; else")))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_TermFieldWithLeadingWhitespace_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/TermFieldWithLeadingWhitespace.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_TermFieldWithTrailingWhitespace_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/TermFieldWithTrailingWhitespace.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_TermFieldWithLeadingAndTrailingWhitespace_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/TermFieldWithLeadingAndTrailingWhitespace.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_NotesFieldWithLeadingWhitespace_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/NotesFieldWithLeadingWhitespace.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "This is a note", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_NotesFieldWithTrailingWhitespace_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/NotesFieldWithTrailingWhitespace.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "This is a note", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    @Test
    fun readAll_When_NotesFieldWithLeadingAndTrailingWhitespace_Should_ReturnListWithOneKeytermWithListOfAlternateRenderingsSplitOnSemiColonsWithExtraWhitespaceRemoved() {
        val keytermCsvReader = getKeytermCsvReaderFromResourcePath("KeytermCsvReader/NotesFieldWithLeadingAndTrailingWhitespace.csv")
        val expectedKeyterms: MutableList<Keyterm> = mutableListOf()
        expectedKeyterms.add(Keyterm("disciple", listOf(), listOf(), "This is a note", listOf()))

        val actualKeyterms = keytermCsvReader.readAll()

        Assert.assertEquals(expectedKeyterms, actualKeyterms)
    }

    private fun getKeytermCsvReaderFromResourcePath(resource: String): KeytermCsvReader{
        val inputStream = this.javaClass.classLoader?.getResourceAsStream(resource)
        val streamReader = InputStreamReader(inputStream)
        return KeytermCsvReader(streamReader)
    }
}