package org.sil.storyproducer.tools.media.sound

class FFmpegReturn(returnCode: Int, commandOutput: String) {
    val rc = returnCode
    val outputString = commandOutput
}