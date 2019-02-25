package org.sil.storyproducer.model

import com.opencsv.CSVReader
import java.io.Reader

class KeytermCsvReader(val reader: Reader){
    private val csvReader = CSVReader(reader)

    init {
        val headers = csvReader.readNext()
        if(headers != null && headers.size < 6){
            csvReader.readAll()
        }
    }

    fun readAll(): List<Keyterm>{
        val keyterms: MutableList<Keyterm> = mutableListOf()
        
        val lines = csvReader.readAll()
        for(line in lines){
            val keyterm = lineToKeyterm(line)
            keyterms.add(keyterm)
        }

        return keyterms
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

    private fun stringToList(field: String, separator: String): List<String>{
        if(field.isNotEmpty()) {
            val list = field.split(separator)
            val trimmedList = list.asSequence().map { it.trim() }.toMutableList()
            trimmedList.remove("")
            return trimmedList
        }
        else{
            return listOf()
        }
    }
}