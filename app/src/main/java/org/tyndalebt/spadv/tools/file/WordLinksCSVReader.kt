import com.opencsv.CSVReader
import org.tyndalebt.spadv.model.WordLink
import java.io.Reader

/**
 * The purpose of this class is to parse wordlink data from a csv file and load into a list of WordLink objects
 *
 * @since 3.1
 * @authors Aaron Cannon, Jake Allinson
 */
class WordLinksCSVReader(reader: Reader): AutoCloseable {
    private val csvReader = CSVReader(reader)

    init {
        val numberOfHeaderRows = 1
        csvReader.skip(numberOfHeaderRows)
    }

    fun readAll(): List<WordLink>{
        val wordLinks: MutableList<WordLink> = mutableListOf()

        val lines = csvReader.readAll()
        for (line in lines) {
            val wordLink = lineToWordLink(line)
            wordLinks.add(wordLink)
        }
        wordLinks.removeAll{ wordlink -> wordlink.term == ""}

        return wordLinks
    }

    override fun close() {
        csvReader.close()
    }

    private fun lineToWordLink(line: Array<String>): WordLink {
        val wordLink = WordLink()

        wordLink.term = line[1].trim()
        wordLink.termForms = stringToList(line[2], ",")
        // DKH - 9/20/2021, for testing process Column D
        wordLink.alternateRenderings = stringToList(line[3], ";") // column D
        // DKH - 9/20/2021, for testing process Column E
        wordLink.explanation = line[4].trim()   // column E
        wordLink.relatedTerms = stringToList(line[5], ",")

        return wordLink
    }

    /*
     * The purpose of this method is to split a string based on a give separator.
     */
    private fun stringToList(field: String, separator: String): List<String>{
        if(field.isNotEmpty()) {
            val list = field.split(separator)
            val trimmedList = list.asSequence().map { it.trim() }.toMutableList()
            //Trim any empty string elements
            trimmedList.removeAll { it == "" }
            return trimmedList
        }
        else{
            return listOf()
        }
    }
}