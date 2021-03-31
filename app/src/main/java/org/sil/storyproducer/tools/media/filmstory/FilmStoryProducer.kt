package org.sil.storyproducer.tools.media.filmstory

import android.net.Uri
import android.widget.Toast
import org.sil.storyproducer.controller.export.FinalizeActivity
import org.sil.storyproducer.R
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.copyToWorkspacePath
import org.sil.storyproducer.tools.file.getStoryChildInputStream
import org.sil.storyproducer.tools.media.Producer
import org.sil.storyproducer.model.VIDEO_DIR
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FilmStoryProducer(override var parent: FinalizeActivity, override var title: String) : Producer, Thread() {
    override var isActive: Boolean = false
    private var slidesCompleted = 0
    var isDone: Boolean = false
    private var isSuccess: Boolean = false
    var progress = 0

    override var progressUpdater: Runnable = Runnable {
        var isDone = false
        while (!isDone) {
            synchronized(FinalizeActivity.storyMakerLock) {
                isDone = this.isDone
                progress = this.progress
            }
            updateProgress((progress.toDouble()/100 * FinalizeActivity.PROGRESS_MAX).toInt())
        }
        parent.runOnUiThread {
            parent.showCreationElements()
            if(this.isSuccess)
                Toast.makeText(parent.baseContext, "Video created!", Toast.LENGTH_LONG).show()
            else
                Toast.makeText(parent.baseContext, "Error!", Toast.LENGTH_LONG).show()
        }

    }

    override fun start() {
        super.start()
    }

    override fun run(){
        super.run()

        //The goal is to patch all the videos and audios together.
        progress = 0
        isActive = true
        val dir = parent.filesDir
        dir.mkdirs()
        val audioList:LinkedList<File> = LinkedList()

        // Step 1: Prepare the audio
        Workspace.activeStory.slides.forEachIndexed { index, slide ->
            if(slide.slideType in arrayOf(SlideType.NUMBEREDPAGE,SlideType.FRONTCOVER)){
                val audioFile = File(dir, "audio$index$MP4_EXT")
                audioFile.createNewFile()
                val finalString = slide.getFinalFileString()
                copyM4aStreamToMp4File(getStoryChildInputStream(parent.baseContext, finalString), audioFile)

                val length = slide.endTime - slide.startTime
                val position = slide.audioPosition
                val slideTempo = calculateSlideTempo(slide, audioFile)
                try{
                    prepareAudio(audioFile, length, position, slideTempo, dir)
                    audioList.add(audioFile)
                }
                catch(e:AudioTooLongException){
                    parent.runOnUiThread {
                        val toaster = Toast.makeText(parent,"Slide #${e.slideNum} audio to long!",Toast.LENGTH_LONG)
                        toaster.show()
                    }
                    isDone = true
                    isActive = false
                    return
                }
            }
            slidesCompleted++
        }

        progress = 10

        // Step 2: Prepare video - Not necessary right now
        val videoFile = File(dir,Workspace.activeStory.fullVideo)
        getFileFromInputStream(getStoryChildInputStream(parent.baseContext,Workspace.activeStory.fullVideo)!!, videoFile)

        val videoWithoutAudio = File(dir, "${videoFile.nameWithoutExtension}_an.mp4")
        removeAudioStream(videoFile, videoWithoutAudio)
        videoFile.delete()

        val videoList = generateVideoFiles(parent, videoWithoutAudio)
        val creditsImage = File(dir,"outputCredits.jpg")

        val commons = parent.baseContext.getString(R.string.license_attribution) +
                SimpleDateFormat("yyyy", Locale.US).format(GregorianCalendar().time)
        createCredits(Workspace.activeStory.localCredits + "\n" + commons,creditsImage)
        val creditsFile = File(dir, "credits.mp4")
        createVideoStillShotFromImage(creditsImage, 5000, creditsFile)
        videoList.addLast(File(dir,"credits.mp4"))

//        val internationalCreditsImage = File(dir,"outputInternationalCredits.jpg")
//        createCredits(commons,internationalCreditsImage)
//        val internationalCreditsFile = File(dir, "internationalCredits.mp4")
//        createVideoStillShotFromImage(internationalCreditsImage, 5000, internationalCreditsFile)
//        videoList.addLast(File(dir,"internationalCredits.mp4"))

        progress = 30

        // Step 3: Create Final Video
        val relPath = title.replace(' ', '_')
        val finalVideo = File(dir,relPath)
        isSuccess = createVideo(audioList,videoList,dir,finalVideo)
        isSuccess = isSuccess && copyToWorkspacePath(parent,Uri.fromFile(finalVideo),"$VIDEO_DIR/$relPath")
        finalVideo.delete()
        Workspace.activeStory.addVideo(relPath)
        progress = 90

        // Step 4: Clean Up
        for(file in audioList){
            file.delete()
        }
        for(file in videoList){
            file.delete()
        }
        creditsImage.delete()
//        internationalCreditsImage.delete()

        // Step 5: Finish Up
        isDone = true
        isActive = false
    }

    companion object {
        const val MP4_EXT = ".mp4"
        const val MP4_ENCODER = "mpeg4"
    }
}
