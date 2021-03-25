package org.sil.storyproducer.tools.media.film

import android.net.Uri
import android.widget.Toast
import org.sil.storyproducer.controller.export.CreateActivity
import org.sil.storyproducer.film.R
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.copyToWorkspacePath
import org.sil.storyproducer.tools.file.getStoryChildInputStream
import org.sil.storyproducer.tools.media.Producer
import org.sil.storyproducer.model.VIDEO_DIR
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FilmProducer(override var parent: CreateActivity, override var title: String) : Producer, Thread() {
    override var isActive: Boolean = false
    var slidesCompleted = 0
    var isDone: Boolean = false
    var isSuccess: Boolean = false
    var progress = 0

    override var progressUpdater: Runnable = Runnable {
        var isDone = false
        var progress = 0
        while (!isDone) {
            synchronized(CreateActivity.storyMakerLock) {
                isDone = this.isDone
                progress = this.progress
            }
            updateProgress((progress.toDouble()/100 * CreateActivity.PROGRESS_MAX).toInt())
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
        var audioList:LinkedList<File> = LinkedList<File>()

        // Step 1: Prepare the audio
        Workspace.activeStory.slides.forEachIndexed { index, slide ->
            if(slide.slideType in arrayOf(SlideType.NUMBEREDPAGE,SlideType.FRONTCOVER)){
                val audioFile = File(dir, "audio$index$MP4_EXT")
                val length = slide.endTime - slide.startTime
                val position = slide.audioPosition
                val slideTempo = calculateSlideTempo(parent.baseContext, dir, slide, false)
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
        var videoList: LinkedList<File> = LinkedList()
        val videoWithoutAudio = File(dir, "${videoFile.nameWithoutExtension}_an.mp4")
        removeAudioStream(videoFile, videoWithoutAudio)
        videoFile.delete()

        videoList = generateVideoFiles(parent, videoWithoutAudio)
        val creditsImage = File(dir,"outputCredits.jpg")
        val internationalCreditsImage = File(dir,"outputInternationalCredits.jpg")
        createCredits(Workspace.activeStory.localCredits,creditsImage)

        var commons = parent.baseContext.getString(R.string.license_attribution) +
                SimpleDateFormat("yyyy", Locale.US).format(GregorianCalendar().time)
        createCredits(commons,internationalCreditsImage)

        val creditsFile = File(dir, "credits.mp4")
        val internationalCreditsFile = File(dir, "internationalCredits.mp4")
        createVideoStillShotFromImage(creditsImage, 5000, creditsFile)
        createVideoStillShotFromImage(internationalCreditsImage, 5000, internationalCreditsFile)
        videoList.addLast(File(dir,"credits.mp4"))
        videoList.addLast(File(dir,"internationalCredits.mp4"))
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
        internationalCreditsImage.delete()

        // Step 5: Finish Up
        isDone = true
        isActive = false
    }

    companion object {
        const val MP4_EXT = ".mp4"
        const val MP4_ENCODER = "mpeg4"
    }
}
