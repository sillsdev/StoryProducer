package org.sil.storyproducer.controller.dramatization

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.adapter.RecordingsList
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.StoryState
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.StorySharedPreferences
import org.sil.storyproducer.tools.file.AudioFiles
import org.sil.storyproducer.tools.file.ImageFiles
import org.sil.storyproducer.tools.file.TextFiles
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.toolbar.PausingRecordingToolbar
import org.sil.storyproducer.tools.toolbar.RecordingToolbar.RecordingListener

import java.io.File


class DramatizationFrag : MultiRecordFrag() {

    private var phaseUnlocked: Boolean = false
    private var slideText: EditText? = null


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater!!.inflate(R.layout.fragment_dramatization, container, false)
        setUiColors()
        setPic(rootView!!.findViewById<View>(R.id.fragment_image_view) as ImageView)
        slideText = rootView!!.findViewById(R.id.fragment_dramatization_edit_text)
        slideText!!.setText(Workspace.activeStory.slides[slideNum].translatedContent, TextView.BufferType.EDITABLE)

        if (phaseUnlocked) {
            rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false)
            closeKeyboardOnTouch(rootView)
            rootView!!.findViewById<View>(R.id.lock_overlay).visibility = View.INVISIBLE
        } else {
            PhaseBaseActivity.disableViewAndChildren(rootView!!)
        }

        phaseUnlocked = StorySharedPreferences.isApproved(Workspace.activeStory.title, context)
        return rootView
    }

    override fun onStart() {
        super.onStart()

        if (phaseUnlocked) {
            setToolbar(rootViewToolbar)
        }
    }

    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on pause.
     */
    override fun onPause() {
        super.onPause()
        closeKeyboard(rootView)
    }

    /**
     * This function serves to handle draft page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is visible to user
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        // Make sure that we are currently visible
        if (this.isVisible) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                if (recordingToolbar != null) {
                    recordingToolbar!!.onPause()
                }
                closeKeyboard(rootView)
            }
        }
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private fun setToolbar(toolbar: View?) {
        if (rootView is RelativeLayout) {
            val recordingListener = object : RecordingListener {
                override fun onStoppedRecording() {}
                override fun onStartedRecordingOrPlayback(isRecording: Boolean) {}
            }

            val rList = RecordingsList(context, this)

            //fix with proper slide num
            recordingToolbar = PausingRecordingToolbar(activity, rootViewToolbar!!, rootView as RelativeLayout,
                    true, false, true, false, rList, recordingListener, 0)
            recordingToolbar!!.keepToolbarVisible()
        }
    }

    /**
     * This function will set a listener to the passed in view so that when the passed in view
     * is touched the keyboard close function will be called see: [.closeKeyboard].
     *
     * @param touchedView The view that will have an on touch listener assigned so that a touch of
     * the view will close the softkeyboard.
     */
    private fun closeKeyboardOnTouch(touchedView: View?) {
        touchedView?.setOnClickListener { closeKeyboard(touchedView) }
    }

    /**
     * This function closes the keyboard. The passed in view will gain focus after the keyboard is
     * hidden. The reestablished focus allows the removal of a cursor or any other focus indicator
     * from the previously focused view.
     *
     * @param viewToFocus The view that will gain focus after the keyboard is hidden.
     */
    private fun closeKeyboard(viewToFocus: View?) {
        if (viewToFocus != null) {
            val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(viewToFocus.windowToken, 0)
            viewToFocus.requestFocus()
        }
        Workspace.activeStory.slides[slideNum].translatedContent = slideText!!.text.toString()
    }

}
