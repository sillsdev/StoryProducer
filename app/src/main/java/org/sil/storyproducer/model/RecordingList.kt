package org.sil.storyproducer.model

class RecordingList {
    val size: Int
        get() = files.size

    val selectedFile: Recording?
        get() = files.getOrNull(selectedIndex) ?: files.getOrNull(0)

    private var files: MutableList<Recording> = ArrayList()
    var selectedIndex: Int = 0

    fun add(recording: Recording) {
        files.add(recording)
        selectedIndex = files.size - 1
    }

    fun removeAt(index: Int) {
        files.removeAt(index)
        if (selectedIndex >= index) {
            selectedIndex--
        }
    }

    fun getFiles() : List<Recording> {
        return files
    }

}
