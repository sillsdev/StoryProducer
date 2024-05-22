package org.sil.storyproducer.controller

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.R
import org.sil.storyproducer.model.PROJECT_DIR
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.copyToFilesDir
import org.sil.storyproducer.tools.file.copyToWorkspacePath
import org.sil.storyproducer.tools.toolbar.MultiRecordRecordingToolbar
import org.sil.storyproducer.tools.toolbar.PlayBackRecordingToolbar
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class MultiRecordFrag : SlidePhaseFrag(), PlayBackRecordingToolbar.ToolbarMediaListener {
    protected open var recordingToolbar: RecordingToolbar = MultiRecordRecordingToolbar()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        setToolbar()

        setupCameraAndEditButton()

        return rootView
    }

    /**
     * Setup camera button for updating background image
     * and edit button for renaming text and local credits
     */
    fun setupCameraAndEditButton() {
        // display the image selection button on FRONTCOVER,LOCALSONG & NUMBEREDPAGE
        // SP422 - DKH 5/6/2022 Enable images on all the slides to be swapped out via the camera tool
        // Add camera tool to numbered pages so that local images can be used in the story
        // If we have a numbered page, only show the camera on the Translate_Revise Phase
        if(!(Workspace.activeStory.slides[slideNum].slideType == SlideType.NUMBEREDPAGE &&
                        Workspace.activePhase.phaseType != PhaseType.TRANSLATE_REVISE)) {
            if (Workspace.activeStory.slides[slideNum].slideType in
                    arrayOf(SlideType.FRONTCOVER, SlideType.LOCALSONG, SlideType.NUMBEREDPAGE)) {
                val imageFab: ImageView = rootView!!.findViewById<View>(R.id.insert_image_view) as ImageView
                imageFab.visibility = View.VISIBLE
                imageFab.setOnClickListener {
                    val chooser = Intent(Intent.ACTION_CHOOSER)
                    chooser.putExtra(Intent.EXTRA_TITLE, R.string.camera_select_from)

                    tempPicFile = File.createTempFile("temp", ".jpg", activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
                    val internalFileUri = FileProvider.getUriForFile(requireContext(), "${BuildConfig.APPLICATION_ID}.fileprovider", tempPicFile!!)

                    val galleryIntent = Intent(Intent.ACTION_PICK)// ACTION_GET_CONTENT)
                    galleryIntent.type = "image/*"
                    galleryIntent.putExtra(MediaStore.EXTRA_OUTPUT, internalFileUri)
                    galleryIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    chooser.putExtra(Intent.EXTRA_INTENT, galleryIntent)

                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, internalFileUri)
                    cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//                    chooser.putExtra(Intent.EXTRA_INTENT, cameraIntent)

                    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                    // grant access to all potential intent activities
                    val resInfoList = requireContext().packageManager.queryIntentActivities(
                        chooser, PackageManager.MATCH_DEFAULT_ONLY)
                    for (resolveInfo in resInfoList) {
                        val packageName = resolveInfo.activityInfo.packageName
                        requireContext().grantUriPermission(packageName, internalFileUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }

                    // try and launch editor for our audio file
                    try {

                        startActivityForResult(chooser, ACTIVITY_SELECT_IMAGE)

                    } catch (e: ActivityNotFoundException) {
                        Log.e("cameraPicture", "intent error: ActivityNotFoundException")
                    }
                }
            }
        }

        // 0R17 - DKH 05/7/2022 Allow for text editing on the song slide
        // display the Edit  button, if on the FRONTCOVER or LOCALSONG
        val slideType : SlideType = Workspace.activeStory.slides[slideNum].slideType
        if(slideType in arrayOf(SlideType.FRONTCOVER,SlideType.LOCALSONG)) {
            //for these, use the edit text button instead of the text in the lower half.
            //In the phases that these are not there, do nothing.
            val editBox = rootView?.findViewById<View>(R.id.fragment_dramatization_edit_text) as EditText?
            editBox?.visibility = View.INVISIBLE

            val editFab = rootView!!.findViewById<View>(R.id.edit_text_view) as ImageView?
            editFab?.visibility = View.VISIBLE
            editFab?.setOnClickListener {
                val editText = EditText(context)
                editText.id = R.id.edit_text_input
                editText.contentDescription = R.string.slide_text_edit.toString()

                // Programmatically set layout properties for edit text field
                val params = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)
                // Apply layout properties
                editText.layoutParams = params
                editText.minLines = 5

                editText.text.insert(0, Workspace.activeSlide!!.translatedContent)

                val dialog = AlertDialog.Builder(context)
                        .setTitle(getString(R.string.enter_text))
                        .setView(editText)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.save) { _, _ ->
                            Workspace.activeSlide!!.translatedContent = editText.text.toString()
                            setPic(rootView!!.findViewById(R.id.fragment_image_view) as ImageView)
                        }.create()

                dialog.show()
            }
        }
        // SP422 - DKH 5/6/2022 Enable images on all the slides to be swapped out via the camera tool
        // Allow the user to restore to the original image
        // If we have a numbered page, only show the restore on the Translate_Revise Phase
        if(slideType == SlideType.NUMBEREDPAGE && Workspace.activePhase.phaseType == PhaseType.TRANSLATE_REVISE) {

            val editFab = rootView!!.findViewById<View>(R.id.restore_image_view) as ImageView?
            editFab?.visibility = View.VISIBLE
            editFab?.setOnClickListener {
                val dialog = AlertDialog.Builder(context)
                        .setTitle(R.string.camera_revert_title)
                        .setMessage(R.string.camera_revert_message)
                        .setNegativeButton(R.string.no, null)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            Workspace.activeStory.slides[slideNum].imageFile = "${slideNum}.jpg"
                            setPic(rootView!!.findViewById(R.id.fragment_image_view) as ImageView)
                        }
                        .create()

                dialog.show()
            }
        }
    }

    fun rotateImageIfRequired(imagePath: String) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        if (bitmap == null)
            return
        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val rotatedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            else -> bitmap
        }

        saveBitmapToFile(rotatedBitmap, imagePath)
    }

    fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }

    fun saveBitmapToFile(bitmap: Bitmap, filePath: String) {
        val file = File(filePath)
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                out?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Change the picture behind the screen.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            if (resultCode == Activity.RESULT_OK && requestCode == ACTIVITY_SELECT_IMAGE) {
                var pictureSetErr = ""
                do {
                    //copy image into workspace
                    var uri = data?.data
                    if (uri == null) {
                        if (tempPicFile == null) {
                            pictureSetErr = "tempPicFile is null!"
                            break
                        }
                        uri = FileProvider.getUriForFile(requireContext(), "${BuildConfig.APPLICATION_ID}.fileprovider", tempPicFile!!)   //it was a camera intent
                    }
                    if (uri == null) {
                        pictureSetErr = "uri is null!"
                        break
                    }
                    // SP422 - DKH 5/6/2022 Enable images on all the slides to be swapped out via the camera tool
                    // Put extension in a common place for use by others

                    // Task 838: rotate image if necessary
                    val rotateTempFolder = File(requireContext().filesDir, "temp_rotate")
                    rotateTempFolder.deleteRecursively()
                    rotateTempFolder.mkdirs()
                    val rotateTempFile = File(rotateTempFolder, "temp_picture_rotate.jpg")
                    copyToFilesDir(requireContext(), uri!!, rotateTempFile)
                    rotateImageIfRequired(rotateTempFile.absolutePath)

                    // Task: 838: now copying from rotated image 'rotateTempFile' instead of original uri
                    Workspace.activeStory.slides[slideNum].imageFile =
                        "$PROJECT_DIR/${slideNum}${Workspace.activeStory.slides[slideNum].localSlideExtension}"
                    copyToWorkspacePath(
                        requireContext(), Uri.fromFile(rotateTempFile),
                        "${Workspace.activeStory.title}/${Workspace.activeStory.slides[slideNum].imageFile}"
                    )
                    rotateTempFile.delete()
                    tempPicFile?.delete()
                    tempPicFile = null

                    setPic(rootView!!.findViewById(R.id.fragment_image_view) as ImageView)

                } while (false)

                if (pictureSetErr.isNotEmpty()) {
                    Toast.makeText(context,"Error: $pictureSetErr",Toast.LENGTH_LONG).show()
                }
            }
        } catch (e:Exception) {
            Toast.makeText(context,"Error Exception: ${e.message}", Toast.LENGTH_LONG).show()
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is currently visible to user
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        // Make sure that we are currently visible
        if (this.isVisible) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                recordingToolbar.stopToolbarMedia()
            }
        }
    }

    protected open fun setToolbar() {
        val bundle = Bundle()
        bundle.putInt(SLIDE_NUM, slideNum)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()

        recordingToolbar.keepToolbarVisible()
    }

    override fun onStartedToolbarMedia() {
        super.onStartedToolbarMedia()

        stopSlidePlayBack()
    }

    override fun onStartedSlidePlayBack() {
        super.onStartedSlidePlayBack()

        recordingToolbar.stopToolbarMedia()
    }
    
    companion object {
        private const val ACTIVITY_SELECT_IMAGE = 53
        private var tempPicFile: File? = null
    }
}
