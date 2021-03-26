package org.sil.storyproducer.tools.media.film

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.getStoryChildInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

private const val VIDEO_MP4_WIDTH = 1280
private const val VIDEO_MP4_HEIGHT = 720

private const val MAX_VIDEO_TEMPO = 1.3

/**
 * @author DSHADE
 * @see #appendVideo(File,File,File) importantNote
 * @see {@link pairAudioToVideo()}
 */

/**
 * Function for printing an error to the debug output
 * @param s a string that appends to base message to help identify the problem
 */
fun errorMessage(s: String) {
    System.err.println("VideoUtility: $s")
}

/**
 * Class for a new Exception so we can pass the problem up the chain of command. See Film Producer
 */
class AudioTooLongException(var slideNum: Int) : Exception()

/**
 * @return whether or not the file is a valid MP4
 */
fun isValidMP4File(file: File): Boolean{
    return file.isFile && file.extension == "mp4"
}

/**
 * Gets the length of an mp4 file in milliseconds.
 *
 * @param file an mp4 file
 * @return the duration of the given file in milliseconds, 0 if the file is invalid
 */
fun getMp4Length(file: File): Int {
    if(!file.exists() || file.length() == 0L) {
        return 0
    }

    val mmr = MediaMetadataRetriever()
    val fis = FileInputStream(file)
    mmr.setDataSource(fis.fd)
    return Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
}

/**
 * Copies the m4a file given by the stream to the mp4 file given by the output. This also "converts"
 * from m4a to mp4.
 *
 * @param m4a the input stream for the m4a file
 * @param mp4 where to put the output file
 * @return true if the operation was successful, false otherwise
 */
fun copyM4aStreamToMp4File(m4a: InputStream?, mp4: File): Boolean{
    if(m4a == null){
        errorMessage("ChangeM4aToMp4 has invalid file")
        return false
    }
    if(isValidMP4File(mp4)){
        val ostream = FileOutputStream(mp4)
        val data = ByteArray(m4a.available())
        m4a.read(data)
        ostream.write(data)
        m4a.close()
        ostream.close()
        return true
    }
    errorMessage("ChangeM4aToMp4 has encountered a problem")
    return false
}

/**
 * Creates a file from an InputStream.
 *
 * @param inputStream the source; an input stream designating the place from which to copy
 * @param outputFile the destination; a file designating the destination of the program
 */
fun getFileFromInputStream(inputStream: InputStream, outputFile: File){
    val outStream = FileOutputStream(outputFile)
    val data = ByteArray(inputStream.available())
    inputStream.read(data)
    outStream.write(data)
    inputStream.close()
    outStream.close()
}

/**
 * Gets the tempo that this slide should use for the audio.
 * @param context an app context is necessary to get the Story's recordings
 * @param parentDir the directory to which to write files
 * @param deleteFiles this function creates (possibly) temporary files to calculate the
 *                      tempo. If the user would like to use those files, they can set
 *                      this flag to false. Else, the function will delete the files.
 *
 * @return a Double in the range [1.0, MAX_VIDEO_TEMPO] representing the tempo to for the audio
 */
fun calculateSlideTempo(context: Context, parentDir: File, slide : Slide,
                        deleteFiles: Boolean = true): Double {
    var newTempo = 1.0

    if(slide.slideType == SlideType.NUMBEREDPAGE || slide.slideType == SlideType.FRONTCOVER) {
        val audioFile = File(parentDir, "audio.mp4")
        audioFile.createNewFile()
        copyM4aStreamToMp4File(getStoryChildInputStream(context, slide.getFinalFile()), audioFile)

        val videoDuration = slide.endTime - slide.startTime
        val narrationDuration = getMp4Length(audioFile)

        if (narrationDuration > videoDuration) {
            newTempo = narrationDuration.toDouble() / videoDuration.toDouble()
            if (newTempo > MAX_VIDEO_TEMPO) {
                newTempo = MAX_VIDEO_TEMPO
            }
        }
        if (deleteFiles) {
            audioFile.delete()
        }
    }

    return newTempo
}

/**
 * This function is to prepare audio to a certain length, before it is merged with the video.
 * @param audioFile the audioFile that is to be prepared
 * @param length the length in millis of how long the video should be
 * @param placeAt the position in millis where the audio should be placed in the track
 * @param directory where any temp files should be placed
 */
fun prepareAudio(audioFile: File, length: Int, placeAt: Int, storyTempo: Double, directory: File?){
    var audioLength = getMp4Length(audioFile) / storyTempo

    val audioTemp = File(directory, "audio-tmp.mp4")
    audioTemp.createNewFile()

    // If length is 0, then the audioFile contains an invalid mp4.
    // This can happen if the user doesn't add audio for a section.
    if(audioLength == 0.0) {
        generateSilence(length, audioFile)
        audioLength = getMp4Length(audioFile).toDouble()
    }

    changeTempo(audioFile, storyTempo, audioTemp)

    audioTemp.copyTo(audioFile, true)
    audioTemp.delete()

    if(audioLength < length) {
        if(placeAt > 0) {
            val temp = File(directory, "tmp.mp4")
            temp.createNewFile()
            // If silence is needed at the beginning, add the silence in
            val startSilence = File(directory, "startsil.mp4")
            generateSilence(placeAt, startSilence)
            concatenateAudioFiles(listOf(startSilence, audioFile), temp)
            temp.copyTo(audioFile, true)
            startSilence.delete()
            temp.delete()
        }

        if(placeAt + audioLength < length) {
            val temp = File(directory, "tmp.mp4")
            temp.createNewFile()
            val endingSilence = File(directory, "lensil.mp4")
            val endingSilenceLength = length - audioLength - placeAt
            generateSilence(endingSilenceLength.toInt(), endingSilence)
            val ret2 = concatenateAudioFiles(listOf(audioFile, endingSilence), temp)
            temp.copyTo(audioFile, true)
            endingSilence.delete()
            temp.delete()
        }
    }
}

/**
 * This function walks through the slides and calculates whether or not a freezeframe is
 * necessary after the tempo adjustment. It returns a list of video files that, in order,
 * represent the entire fullVideo given, but split up into clips including the freeze
 * frame.
 *
 * @param context the context is necessary to read the Story's dramatization files
 * @param fullVideo the file that points to the video file
 * @param storyTempo how fast the audio will be sped up
 * @return a linked list containing all of the video clips that comprise the whole
 */
fun generateVideoFiles(context: Context, fullVideo: File): LinkedList<File> {
    val dir = fullVideo.parentFile
    val videos: LinkedList<File> = LinkedList()
    var remainingVideo = fullVideo
    var previousCutTime = 0.0

    Workspace.activeStory.slides.forEachIndexed { index, slide ->
        if(slide.slideType == SlideType.NUMBEREDPAGE || slide.slideType == SlideType.FRONTCOVER) {
            val audioFile = File(context.filesDir, "tmp-audio.mp4")
            audioFile.createNewFile()
            copyM4aStreamToMp4File(getStoryChildInputStream(context,
                    slide.chosenVoiceStudioFile.substringAfter("|")), audioFile)
            val narrationDuration = getMp4Length(audioFile)
            val videoDuration = slide.endTime - slide.startTime
            val slideTempo = calculateSlideTempo(context, dir!!, slide, false)
            val adjustedNarrationDuration = narrationDuration / slideTempo
            audioFile.delete()

            if (adjustedNarrationDuration > videoDuration) {
                val freezeFrameDuration = adjustedNarrationDuration - videoDuration
                val cutTime = slide.endTime

                val startClip = File(dir, "slide$index-start.mp4")
                trimDurationToMillis(remainingVideo, cutTime - previousCutTime.toInt(), startClip)

                val frame = File(dir, "tmp-frame.png")
                val still = File(dir, "slide$index-still.mp4")
                val frameRet = extractFrameAt(fullVideo, cutTime, frame)
                val ret = createVideoStillShotFromImage(frame, freezeFrameDuration.toInt(), still)
                frame.delete()

                videos.addLast(startClip)
                videos.addLast(still)

                if(index != Workspace.activeStory.numSlides - 1) {
                    val remaining = File(dir, "after-slide$index.mp4")
                    moveStartToMillis(fullVideo, cutTime, remaining)
                    remainingVideo = remaining
                }

                previousCutTime = freezeFrameDuration + cutTime
            }
        }
    }

    videos.addLast(remainingVideo)

    return videos
}

/**
 * Generates an audio file containing only silence for the specified length.
 *
 * @param duration the number of milliseconds for the silence to last
 * @param output the location of the output
 * @return an FFmpegReturn object representing the command's output
 */
fun generateSilence(duration: Int, output: File): FFmpegReturn {
    val seconds = duration / 1000.0

    /**
     * FFmpeg command:
     *   -y                 Overwrites the output file if it already exists
     *   -f lavfi           Specifies the output format
     *   -i aevalsrc=0      Tells ffmpeg to use a null source for the input (silence)
     *   -t { seconds }     Tells ffmpeg to only go for the specified number of seconds
     *   -vn                Tells ffmpeg to use not use a video stream in the output file
     */
    val command = "-y -f lavfi -i aevalsrc=0 -t $seconds -vn ${output.absolutePath}"
    return FFmpegReturn(command)
}

/**
 * Creates a waveform image for the input file's audio.
 *
 * @param input the input audio from which to generate the image
 * @param output the location of the waveform image
 * @return an FFmpegReturn object representing the command's output
 */
fun generateWaveformImage(input: File, output: File): FFmpegReturn {
    /**
     * FFmpeg command:
     *   -y                 Overwrites the output file if it already exists
     *   -i { imagePath }   Specifies the input path for the image
     *
     *   -- Complex filter (only the audio channel):
     *      showwavespic (https://ffmpeg.org/ffmpeg-filters.html#showwavespic)
     *          s=640x120      - sets the dimensions of the image to 640x120. The default is 640x240,
     *                               so this is default width, half height
     *          colors=#ffffff - sets the color of the waveform to white
     */
    val command = "-y -i ${input.absolutePath} -filter_complex:a \"showwavespic=s=640x120:colors=#ffffff\" ${output.absolutePath}"
    return FFmpegReturn(command)
}

/**
 * Creates a video that just shows the same image for the given amount of milliseconds.
 *
 * @param input the image to create a video from
 * @param milliseconds how long the resulant video should be in milliseconds
 * @param output the output path for the resultant video
 * @return an FFmpegReturn object
 */
fun createVideoStillShotFromImage(input: File, milliseconds: Int, output: File): FFmpegReturn {
    val seconds = milliseconds / 1000.0

    /**
     * FFmpeg command:
     *   -y                 Overwrites the output file if it already exists
     *   -loop 1            Tells FFmpeg to loop on the first frame indefinitely
     *   -i { imagePath }   Specifies the input path for the image
     *   -t { seconds }     How long to make the video in seconds
     *   -an                Tells FFmpeg to not use an audio stream (no audio)
     *   { outputPath }     The path for the resultant video
     */
    val command = "-y -loop 1 -i ${input.absolutePath} -t $seconds -an ${output.absolutePath}"
    return FFmpegReturn(command)
}

/**
 * Pulls the last frame off of the input file, and saves it to the output file.
 *
 * @param input the video file from which to get the last frame
 * @param output the file to which to save the last frame
 * @return an FFmpegReturn object
 */
fun extractFrameAt(input: File, millisecond: Int, output: File) : FFmpegReturn {
    /**
     * FFmpeg command:
     *   -ss { second }     Scans to the specified second in the video
     *   -i { path }        Specifies the input file
     *   -frames:v 1        Tells FFmpeg to only use one frame from the video stream
     *   { outputPath }     Specifies the output path
     */
    val second = millisecond / 1000.0
    val command = "-ss $second -i ${input.absolutePath} -frames:v 1 ${output.absolutePath}"
    return FFmpegReturn(command)
}

/**
 * Changes the tempo of the given audio file, and puts the output in the output file. FFmpeg can't
 * do just any tempo. Per FFmpeg's docs, you can only have 0.5 < tempo < 100.0:
 *      https://ffmpeg.org/ffmpeg-all.html#atempo
 * @param input the input audio file to slow down
 * @param tempo the new speed of the audio
 * @param output the location to save the new audio file
 * @return an FFmpegReturn object that represents this command
 */
fun changeTempo(input: File, tempo: Double, output: File): FFmpegReturn {
    /**
     * FFmpeg command:
     *   -y                         Overwrites the output file if it exists
     *   -i { path }                Specifies the input file
     *   -ac { num }                Sets the number of audio channels; this command chooses mono
     *   -filter:a { filter }       Begins an audio filter
     *      - atempo = { tempo }    Tells FFmpeg to use a tempo filter
     *   -vn                        Tells FFmpeg not to include a video stream (output has no video)
     *   { outputPath }             Specifies the output path
     */
    val tempoString = String.format("%.9f", tempo)
    val command = "-y -i ${input.absoluteFile} -ac 1 -filter:a atempo=$tempoString -vn ${output.absoluteFile}"
    return FFmpegReturn(command)
}

/**
 * Cuts the input file to match the given number of milliseconds.
 *
 * @param input the input file to trim
 * @param millis the number of milliseconds to trim the file to
 * @param output where to output the trimmed file
 * @return an FFmpegReturn object representing the command
 */
fun trimDurationToMillis(input: File, millis: Int, output: File): FFmpegReturn {
    /**
     * FFmpeg command:
     *   -i { path }        Specifies the input file
     *   -t { seconds }     Specifies how long the resultant video should be
     *   -c copy            Tells FFMpeg to use the lightning-fast copy codec
     *   { outputPath }     Specifies the output path
     */
    val seconds = millis / 1000.0
    val command = "-i ${input.absolutePath} -t $seconds -c copy ${output.absolutePath}"
    return FFmpegReturn(command)
}

/**
 * Trims time of the start of a video. Moves the "start time" to the specified millis.
 *
 * @param input the video to trim
 * @param millis where the new start of the video will be
 * @param output the resultant file location
 * @return an FFmpegReturn object representing this command's output
 */
fun moveStartToMillis(input: File, millis: Int, output: File): FFmpegReturn {
    /**
     * FFmpeg command:
     *   -i { path }        Specifies the input file
     *   -ss { seconds }    Scans to this second on the input file
     *   -c copy            Tells FFMpeg to use the lightning-fast copy codec
     *   { outputPath }     Specifies the output path
     */
    val seconds = millis / 1000.0
    val command = "-i ${input.absolutePath} -ss $seconds -c copy ${output.absolutePath}"
    return FFmpegReturn(command)
}

/**
 * Removes the audio stream from a file.
 *
 * @param input the file to remove the audio from
 * @param output where to save the resultant file
 * @return an FFmpegReturn object representing the result of this command
 */
fun removeAudioStream(input: File, output: File): FFmpegReturn {
    /**
     * FFmpeg command:
     *   -i { path }        Specifies the input file
     *   -vcodec { codec }  Specifies which video codec to use. This command uses the copy codec,
     *                          which just copies the old file into the new one. The copy codec is
     *                          lightening fast, but it doesn't do any re-encoding.
     *   -an                Tells FFmpeg to not use an audio stream
     *   { outputPath }     Specifies the output path
     */
    val command = "-i ${input.absolutePath} -vcodec copy -an ${output.absolutePath}"
    return FFmpegReturn(command)
}

/**
 * Concatenates all the videos in the video list into one video, and outputs to the given output
 * File location.
 *
 * @param files the videos to concatenate together
 * @param output the output path for the resultant video
 * @return an FFmpegReturn object for this command
 */
fun concatenateVideoFiles(files: List<File>, output: File): FFmpegReturn {
    /**
     * FFmpeg command:
     *   This command is fairly complex, as it uses a complex filter.
     *      https://ffmpeg.org/ffmpeg-all.html#Complex-filtergraphs
     *      https://trac.ffmpeg.org/wiki/Concatenate
     * The command is programmatically generated, so putting the exact command here is too much. The
     * complex filter follows a general format:
     *   1. Specify the input files
     *   2. Tell ffmpeg what streams to take from the input file
     *   3. Run the concat filter and name the output stream
     *   4. Map that output stream to a file.
     *
     * Command breakdown:
     *      -i { file } -i { file } ...     Each input file is specified with the -i flag
     *      -filter_complex "               Begin the filter_complex command
     *          [0:0][1:0]...               This is not technically part of the filter. It specifies
     *                                          the streams to take from the input files.The format
     *                                          is [sourceFile:streamNumber]. So, [1:0] refers to
     *                                          stream 0 of the second input file. (zero-indexing)
     *          concat=n={ numFiles }       This is the actual concat filter. You begin by
     *                                          specifying how many input files there are.
     *          :v=1:a=0[out]               This says that the filter will have one video output
     *                                          stream and no audio output streams. The name of the
     *                                          stream out of the filter is "out".
     *      -map "[out]"                    This begins the mapping to the final output. The docs
     *                                          form mapping: https://trac.ffmpeg.org/wiki/Map
     *                                          This command tells ffmpeg to use the [out] stream
     *                                          that was the result of the filter as the stream for
     *                                          the final output file.
     *      -r 30                           Tells ffmpeg to use a framerate of 30. This flag fixes
     *                                          some issues with timebase if you have videos of
     *                                          different formats
     *      -q:v 1                          The quality flag (q:v = quality for video) takes a number
     *                                          from 1 to 35, with 1 meaning highest quality and 35
     *                                          being the lowest. This flag is necessary so that the
     *                                          concatenation doesn't degrade the quality if there
     *                                          are changes in bitrate, codec, etc.
     */
    // Put the files in the form -i file -i file -i file
    val inputValues = files.joinToString(" -i ", "-i ") { it.absolutePath }

    var filter = "-filter_complex \""
    filter += files.withIndex().joinToString("") { "[${it.index}:0]" }
    filter += "concat=n=${files.size}:v=1:a=0[out]\""

    val command = "$inputValues $filter -map \"[out]\" -r 30 -q:v 1 ${output.absolutePath}"

    return FFmpegReturn(command)
}

/**
 * Concatenates the list of audio files into the specified output file.
 *
 * @param files the input files to concatenate together
 * @param output the file location to place the resultant video
 * @return an FFmpegReturn object representing this command
 */
fun concatenateAudioFiles(files: List<File>, output: File): FFmpegReturn {
    /**
     * FFMpeg Command:
     *  This command uses a complex filter to concat the files together. See concatenateVideoFiles
     *  for a breakdown of the filter_complex command.
     */
    val inputValues = files.joinToString(" -i ", "-i ") { it.absolutePath }

    var filter = "-filter_complex \""
    filter += files.withIndex().joinToString("") { "[${it.index}:0]" }
    filter += "concat=n=${files.size}:v=0:a=1[out]\""

    val command = "$inputValues $filter -map \"[out]\" ${output.absolutePath}"

    return FFmpegReturn(command)
}

/**
 * Adds an audio stream to a video file.
 *
 * @param videoFile the video to which to attach the audio
 * @param audioFile the audio to attach to the video
 * @param output the place to save the resultant video
 * @return an FFmpegReturn object containing this command's output
 */
fun addAudioToVideo(videoFile: File, audioFile: File, output: File) : FFmpegReturn {
    /**
     * FFmpeg command:
     *   -i { path }        Specifies the input file
     *   -map 0:v:0         Grabs the video stream from the first file
     *   -map 1:a:0         Grabs the audio stream from the second file
     *   { outputPath }     Specifies the output path
     */
    val command = "-i ${videoFile.absolutePath} -i ${audioFile.absolutePath} -c copy -map 0:v:0 -map 1:a:0 ${output.absolutePath}"
    return FFmpegReturn(command)
}

/**
 * This function is the new design.
 * The purpose is to line up all the audio and video clips in order and this will create the video.
 * @param audioFiles the audio files for the final video in order
 * @param videoFiles the video files for the final video in order
 * @param directory the temp/working directory where the function can
 * @param output where the final video should be put
 */
fun createVideo(audioFiles: List<File>, videoFiles: List<File>, directory: File?, output: File): Boolean{
    try{
        //There could be a litany of issues with the files you have added you can use this break to check them
        directory!!.mkdirs()

        val concatenatedVideo = File(directory, "concatenatedVideo.mp4")
        concatenatedVideo.createNewFile()
        val videoReturn = concatenateVideoFiles(videoFiles, concatenatedVideo)

        val concatenatedAudio = File(directory, "concatenatedAudio.mp4")
        concatenatedAudio.createNewFile()
        val audioReturn = concatenateAudioFiles(audioFiles, concatenatedAudio)

        val audioVideoReturn = addAudioToVideo(concatenatedVideo, concatenatedAudio, output)

        concatenatedVideo.delete()
        concatenatedAudio.delete()

        return true
    }
    catch(e:Exception){
        errorMessage("The function createVideo has encountered a problem!")
        return false
    }
}

/**
 * Creates a credits image from the input string.
 *
 * @param creditString the text to put on the credits
 * @param output the file to save the credits image to
 */
fun createCredits(creditString: String, output: File){

    // Split Credit String by newline
    var lines = ArrayList<String>()
    for(line in creditString.split("\n")) {

        // Limit the amount of chars in the local credits
        if(line.contains(":") && line.length > 34) {
            val newLines = line.split(":").toMutableList();
            newLines[0] += ":";
            lines.addAll(newLines)
        } else {
            lines.add(line)
        }
    }

    val lineHeight = VIDEO_MP4_HEIGHT/lines.size
    val bitmap = Bitmap.createBitmap(VIDEO_MP4_WIDTH, VIDEO_MP4_HEIGHT,Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawARGB(0,0,0,0)
    val paint = Paint()
    canvas.drawPaint(paint)
    paint.color = Color.WHITE
    paint.textSize = (48).toFloat()
    paint.textAlign = Paint.Align.CENTER
    for(line in lines){
        val index = lines.indexOf(line)
        val xpos = (VIDEO_MP4_WIDTH/2).toFloat()
        val temp = (lineHeight*(index+1))
        val ypos = ((temp/2) - ((paint.descent()+paint.ascent())/2))
        canvas.drawText(line,xpos,ypos,paint)
    }

    canvas.save()
    bitmap.compress(Bitmap.CompressFormat.JPEG,100,output.outputStream())
}
