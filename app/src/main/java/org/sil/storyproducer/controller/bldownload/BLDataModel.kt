package org.sil.storyproducer.controller.bldownload

// This class implements the data for each available download in the static list of BL download card items
class BLDataModel(var title: String,    // The title for the Bloom Library story template
                  var lang: String,     // The source language code for the story
                  var imageId: Int,     // TODO: This is currently an app resource id but needs to be a downloaded .jpg string for a story thumbnail
                  var downloadUri: String,  // The Url to use to download the story (zipped with a .bloomSource file extension)
                  var isInWorkspace: Boolean = false,   // This is true if the story has already been installed
                  var isInBLDLDir: Boolean = false,   // This is true if the story archive has already been downloaded
                  var isChecked: Boolean = false,       // This is checked by the UI to indicate the story should be downloaded
                  var downloadId: Long = DOWNLOAD_NOT_REQUESTED // If being downloaded this is the value given by the DownloadManager
)
{
    companion object {
        // these 'literals' are used in the downloadId member to indicate a state if not currently being downloaded.
        // If currently being downloaded then a valid downloadId will be >= 0
        const val DOWNLOAD_NOT_REQUESTED = -1L
        const val ALREADY_DOWNLOADED = -2L
        const val DOWNLOADED_COMPLETE = -3L
    }

    val isEnabled : Boolean
    get() { return !isInWorkspace && !isInBLDLDir && (downloadId == BLDataModel.DOWNLOAD_NOT_REQUESTED) }

}
