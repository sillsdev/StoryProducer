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
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.copyToFilesDir
import org.sil.storyproducer.tools.file.copyToWorkspacePath
import org.sil.storyproducer.tools.file.deleteStoryFile
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
     *
     * 24 Sep 2024 Peter C. - Bugfix: 842 - Revert Image does not work for Bloom books
     * -------------------------------------------------------------------------------
     * The algorithm for selecting a new filename for images selected by the camera tool or camera roll
     * was only working for Bible Story Templates and not for Bloom Books.  This was due to an over simplification
     * of what was needed to create a new filename for the camera image that would also allow the original filename
     * of a story slide image to be determined.
     *
     *      ORIGINAL FILENAME      CAMERA TOOL FILENAME
     *      -------- --------      ------ ---- --------
     *
     * In SP 4.2.2: only the slide number was needed to restore the original slide image filename.
     * NB: "_Local.png" was always added after the slide number of the image even if the image was really a .jpg
     * format file and to regenerate the original image the slide number followed by ".jpg" was always used. i.e.:
     *
     *      "<slide_no>.jpg"  <=>  "project/<slide_no>_Local.png"
     *
     * In SP 4.2.3: a new filename is created that is backwards compatible with 4.2.2 in that when referencing
     * a selected Bible Story camera image a "_Local.jpg" postfix is added to the slide number of the original
     * Bible Story slide image name.  However, Bible Story camera selected image filenames ending with either
     * "_Local.jpg" and "_Local.png" are both converted back to the slide number followed by ".jpg".
     * NB: In the camera tool image selection code the image is rotated and always converted to .jpg format.
     * Going forward this should now prevent confusion over the true image file type based on the file extension:
     *
     *      "<slide_no>.jpg"  <=   "project/<slide_no>_Local.png"
     *      "<slide_no>.jpg"  <=>  "project/<slide_no>_Local.jpg"
     *
     * This still leaves us with how to generate camera selected filenames for Bloom Book projects.  However,
     * this can now be done in a similar way to Bible Story new filenames in that a "_Local.jpg" is used
     * instead of the original ".jpg" file extension if the original slide image has a ".jpg" extension:
     *
     *      "<basename>.jpg"  <=>  "project/<basename>_Local.jpg"
     *
     * However, if the original Bloom Book slide image filename is not a .jpg file then to preserve the
     * original extension an extra file extension matching the original slide image file extension is added to
     * the new filename before the final ".jpg" file extension.  So, for example, if the original slide image
     * file was a .png file then the conversion would be as follows:
     *
     *      "<basename>.png"  <=>  "project/<basename>_Local.png.jpg"
     *
     * This now only leaves us with how to convert between a blank image filename indicating a background gray
     * image to a new filename for holding camera selected background images on the title and song slides.  This still
     * is done by using a "<slide_no>_Local.jpg" filename, but for all title and song slides the image filename
     * is reverted back to an empty "" string which indicates to the drawing code that a gray patterned image should
     * be used for that slide:
     *
     *      ""                <=>  "project/<slide_no>_Local.jpg"
     *
     * Finally, when reverting slide images the previously selected camera image is deleted to prevent clutter of
     * the "project" sub-folder and to save storage space.  Should to user want to use the same picture again then
     * they will have to re-select the image from the camera roll.
     *
     */
    fun setupCameraAndEditButton() {
        // display the image selection button on FRONTCOVER,LOCALSONG & NUMBEREDPAGE
        // SP422 - DKH 5/6/2022 Enable images on all the slides to be swapped out via the camera tool
        // Add camera tool to numbered pages so that local images can be used in the story
        // If we have a numbered page, only show the camera on the Translate_Revise Phase
        val phaseType = Workspace.activePhase.phaseType
        val slideType = Workspace.activeStory.slides[slideNum].slideType
        val imageFile = Workspace.activeStory.slides[slideNum].imageFile
        // get the (single) extension of the potentially swapped out image filename
        val imageExtension = imageFile.substringAfterLast(".", "")   // the file extension without '.'
        // find a double file extension if there is one (e.g.: ".png.jpg") the first one being the original file extension otherwise simply use the single file extension
        val imageDblExtFind = Regex("\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9]+\$").find(imageFile)?.value?.substring(1) ?: imageExtension
        // set a flag if an image was previously selected by the camera tool
        val restoreImageEnabled = imageFile.startsWith("$PROJECT_DIR/") &&
                imageFile.endsWith("${Slide.localSlideExtension}${imageDblExtFind}") // true if a slide image was replaced
        // set a flag if it was previously a background image
        val restoreBackgroundEnabled = slideType in arrayOf(SlideType.FRONTCOVER, SlideType.LOCALSONG)
        // Check to see if the camera FAB should be enabled
        if ((phaseType == PhaseType.TRANSLATE_REVISE &&
                    slideType in arrayOf(SlideType.FRONTCOVER, SlideType.LOCALSONG, SlideType.NUMBEREDPAGE)) ||
            (phaseType != PhaseType.TRANSLATE_REVISE &&
                    slideType in arrayOf(SlideType.FRONTCOVER, SlideType.LOCALSONG))) {
            val imageFab = rootView!!.findViewById<ImageView>(R.id.insert_image_view)
            imageFab.visibility = if (restoreImageEnabled) View.INVISIBLE else View.VISIBLE  // only visible if not already replaced
            if (!restoreImageEnabled) { // Only add a click listener to the Image button if visible/enabled
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
        if (slideType in arrayOf(SlideType.FRONTCOVER, SlideType.LOCALSONG)) {
            //for these, use the edit text button instead of the text in the lower half.
            //In the phases that these are not there, do nothing.
            val editBox = rootView?.findViewById<EditText>(R.id.fragment_dramatization_edit_text)
            editBox?.visibility = View.INVISIBLE

            val editFab = rootView!!.findViewById<ImageView>(R.id.edit_text_view)
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
        // Check to see if the restore image FAB should be enabled
        if ((phaseType == PhaseType.TRANSLATE_REVISE &&
                    slideType in arrayOf(SlideType.FRONTCOVER, SlideType.LOCALSONG, SlideType.NUMBEREDPAGE)) ||
            (phaseType != PhaseType.TRANSLATE_REVISE &&
                    slideType in arrayOf(SlideType.FRONTCOVER, SlideType.LOCALSONG))) {
            val restoreImageFab = rootView!!.findViewById<View>(R.id.restore_image_view)
            restoreImageFab?.visibility = if (restoreImageEnabled) View.VISIBLE else View.INVISIBLE  // only visible if already replaced
            if (restoreImageEnabled) {   // Only add a click listener to the restore button if visible/enabled
                var restoredImageFile = ""  // the original image file names for the gray background slides are an empty string
                if (!restoreBackgroundEnabled) {
                    // not restoring to a (blank) background image so we need to determine the original filename from the current imageFile name
                    // i.e. remove "project/" prefix and "_Local." suffix to base file name
                    restoredImageFile = imageFile.replace(Regex("^project/"), "")
                    // get the original file extension if it was added as a double image file extension (e.g.: for ".png.jpg" use ".png")
                    var imageExtOriginal = imageDblExtFind.substringBeforeLast(".", "")
                    if (imageExtOriginal.isEmpty()) {
                        // However, if only a single extension was used then restore to .jpg extension
                        if (imageExtension == "png")
                            imageExtOriginal = "jpg"    // old code always used _Local.png for a camera tool selected file (even if it was really a .jpg)
                        else
                            imageExtOriginal = imageExtension   // no double file extension so use the single filename extension (which should be .jpg)
                    }
                    // Now finish getting the original image filename for use when restoring the image in the onClickListener below
                    restoredImageFile = restoredImageFile
                        .replace(Regex("${Slide.localSlideExtension.substringBeforeLast(".")}\\.${imageDblExtFind}\$"), ".${imageExtOriginal}")
                }
                restoreImageFab?.setOnClickListener {
                    val dialog = AlertDialog.Builder(context)
                            .setTitle(R.string.camera_revert_title)
                            .setMessage(R.string.camera_revert_message)
                            .setNegativeButton(R.string.no, null)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                val cameraToolFile = Workspace.activeStory.slides[slideNum].imageFile
                                Workspace.activeStory.slides[slideNum].imageFile = restoredImageFile
                                setPic(rootView!!.findViewById(R.id.fragment_image_view) as ImageView)
                                setupCameraAndEditButton()  // here this will show the camera button and hide the restore image button
                                deleteStoryFile(requireContext(), cameraToolFile)   // Delete the camera tool selected file now that it is no longer referenced
                            }
                            .create()

                    dialog.show()
                }
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
                    var imageExtension = "" // the filename extension without a '.'
                    val imageFile = Workspace.activeStory.slides[slideNum].imageFile
                    if (imageFile.isEmpty()) {
                        // set the new background image file name to be used for the slide as (for example): "project/1_Local.jpg"
                        // this avoids slide number clashes and allows the filename to be restored to an empty string on restore button pressed
                        imageExtension = "jpg"
                        // Set the new imageFile path to point to the camera tool selected and rotated file
                        Workspace.activeStory.slides[slideNum].imageFile = "$PROJECT_DIR/${slideNum}${Slide.localSlideExtension}${imageExtension}"
                    } else {
                        // get the file extension (without '.') from the original image filename
                        imageExtension = imageFile.substringAfterLast(".", "")
                        var imageDblExtension = imageExtension
                        if (imageDblExtension != "jpg")
                            imageDblExtension = "$imageDblExtension.jpg"  // this creates the double extension starting with the original extension
                        // add a "project/" prefix and "_Local" postfix to the base filename
                        // Set the new imageFile path to point to the camera tool selected and rotated file
                        Workspace.activeStory.slides[slideNum].imageFile = "$PROJECT_DIR/${imageFile}"
                            .replace(Regex("\\.${imageExtension}\$"), "${Slide.localSlideExtension}${imageDblExtension}")
                    }
                    // copy the selected and rotated file to the localtion already set in the slide's imageFile property
                    copyToWorkspacePath(
                        requireContext(), Uri.fromFile(rotateTempFile),
                        "${Workspace.activeStory.title}/${Workspace.activeStory.slides[slideNum].imageFile}"
                    )
                    rotateTempFile.delete()
                    tempPicFile?.delete()
                    tempPicFile = null

                    setPic(rootView!!.findViewById(R.id.fragment_image_view) as ImageView)

                    setupCameraAndEditButton()  // here this will hide the camera button and show the restore image button

                } while (false)

                if (pictureSetErr.isNotEmpty()) {
                    Toast.makeText(context,"Error: $pictureSetErr",Toast.LENGTH_LONG).show()
                    FirebaseCrashlytics.getInstance().log("Error: (ACTIVITY_SELECT_IMAGE) $pictureSetErr")
                }
            }
        } catch (e:Exception) {
            Toast.makeText(context,"Error (ACTIVITY_SELECT_IMAGE) Exception: ${e.message}", Toast.LENGTH_LONG).show()
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
