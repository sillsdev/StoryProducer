package org.sil.storyproducer.model

import com.opencsv.CSVReader
import java.io.Reader

/**
 * The purpose of this class is to parse keyterm data from a csv file and load into a list of Keyterm objects
 *
 * @since 2.6 Keyterm
 * @author Aaron Cannon
 */
class KeytermCsvReader(reader: Reader): AutoCloseable{
    private val csvReader = CSVReader(reader)
    
    init {
        val numberOfHeaderRows = 1
        csvReader.skip(numberOfHeaderRows)
    }

    fun readAll(): List<Keyterm>{
        val keyterms: MutableList<Keyterm> = mutableListOf()

        val lines = csvReader.readAll()
        for (line in lines) {
            val keyterm = lineToKeyterm(line)
            keyterms.add(keyterm)
        }

        return keyterms
    }

    override fun close() {
        csvReader.close()
    }

    private fun lineToKeyterm(line: Array<String>): Keyterm{
        val keyterm = Keyterm()

        keyterm.term = line[1].trim()
        keyterm.termForms = stringToList(line[2], ",")
        keyterm.alternateRenderings = stringToList(line[3], ";")
        keyterm.explanation = line[4].trim()
        keyterm.relatedTerms = stringToList(line[5], ",")

        return keyterm
    }

    /*
     * The purpose of this method is to split a string based on a give separator.
     */
    private fun stringToList(field: String, separator: String): List<String>{
        if(field.isNotEmpty()) {
            val list = field.split(separator)
            val trimmedList = list.asSequence().map { it.trim() }.toMutableList()
            //Trim any empty string elements
            trimmedList.remove("")
            return trimmedList
        }
        else{
            return listOf()
        }
    }
}