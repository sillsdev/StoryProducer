package org.sil.storyproducer.model

import java.util.*

/**
 * BLBookList is used as a collection of books available to download from BloomLibrary
 */
open class BLBookList(var dateUpdated: Date) {
    var booklist = mutableListOf<BLBook>()

    init {
        booklist.add(BLBook("001 The Widow’s Offering", "en",
            "https://api.bloomlibrary.org/v1/fs/harvest/A7cc3l3E5u/thumbnails/thumbnail-256.png?version=2022-08-17T17:36:38.898Z&amp;ref=sil-spapp",
            "https://api.bloomlibrary.org/v1/fs/harvest/A7cc3l3E5u/001+The+Widow%e2%80%99s+Offering.bloomSource?ref=sil-spapp"))
        booklist.add(BLBook("002 Lost Coin", "en",
            "https://api.bloomlibrary.org/v1/fs/harvest/axp6zFqdGH/thumbnails/thumbnail-256.png?version=2022-08-17T17:53:50.678Z&amp;ref=sil-spapp",
            "https://api.bloomlibrary.org/v1/fs/harvest/axp6zFqdGH/002+Lost+Coin.bloomSource?ref=sil-spapp"))
        booklist.add(BLBook("006 Snakes Secret", "en",
            "https://api.bloomlibrary.org/v1/fs/harvest/pdOADFmREf/thumbnails/thumbnail-256.png?version=2022-08-17T18:03:02.753Z&amp;ref=sil-spapp",
            "https://api.bloomlibrary.org/v1/fs/harvest/pdOADFmREf/006+Snakes+Secret.bloomSource?ref=sil-spapp"))
        booklist.add(BLBook("007 Leopards Kill", "en",
            "https://api.bloomlibrary.org/v1/fs/harvest/lE7Gj1se23/thumbnails/thumbnail-256.png?version=2022-08-17T17:59:58.649Z&amp;ref=sil-spapp",
            "https://api.bloomlibrary.org/v1/fs/harvest/lE7Gj1se23/007+Leopards+Kill.bloomSource?ref=sil-spapp"))
        booklist.add(BLBook("103 Babel Languages", "en",
            "https://api.bloomlibrary.org/v1/fs/harvest/MDyjSjSmPe/thumbnails/thumbnail-256.png?version=2022-08-17T17:45:22.243Z&amp;ref=sil-spapp",
            "https://api.bloomlibrary.org/v1/fs/harvest/MDyjSjSmPe/103+Babel+Languages.bloomSource?ref=sil-spapp"))
    }
}

class BLBook(
        val Title: String,
        val LangCode: String,
        val ThumbnailURL: String,
        val BloomSourceURL: String)

object BlBookList : BLBookList(Date())

fun parseOPDSfile(): MutableList<BLBook> {
     return BlBookList.booklist
}
