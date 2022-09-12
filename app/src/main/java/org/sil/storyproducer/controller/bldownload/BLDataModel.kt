package org.sil.storyproducer.controller.bldownload

// This class implements the data for each available download in the static list of BL download card items
class BLDataModel(var title: String,    // The title for the Bloom Library story template
                  var lang: String,     // The source language code for the story
                  var imageId: Int,     // TODO: This is currently an app resource id but needs to be a downloaded .jpg string for a story thumbnail
                  var downloadUri: String,  // The Url to use to download the story (zipped with a .bloomSource file extension)
                  var isInWorkspace: Boolean = false,   // This is true if the story has already been downloaded or installed
                  var isChecked: Boolean = false,       // This is checked by the UI to indicate the story should be downloaded
                  var downloadId: Long = NOT_DOWNLOADED // If being downloaded this is the value given by the DownloadManager
)
{
    companion object {
        val NOT_DOWNLOADED = -1L
        val ALREADY_DOWNLOADED = -2L
    }
}
