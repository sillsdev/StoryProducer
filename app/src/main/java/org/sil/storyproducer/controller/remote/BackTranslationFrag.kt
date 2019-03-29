package org.sil.storyproducer.controller.remote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.graphics.drawable.VectorDrawableCompat
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.Network.VolleySingleton
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import java.util.*

/**
 * Created by alexamhepner on 10/23/17.
 */

class BackTranslationFrag : MultiRecordFrag(), RecordingToolbar.RecordingListener {

    private var slideNumber: Int = 0
    private var storyName: String? = null
    private var phaseUnlocked = false

    private var transcriptionText: EditText? = null

    private var obj: JSONObject? = null
    private var resp: String? = null
    private var js: MutableMap<String, String>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_backtranslation, container, false)
        setPic(rootView!!.findViewById(R.id.fragment_backtranslation_image_view))
        setCheckmarkButton(rootView!!.findViewById(R.id.fragment_backtranslation_r_concheck_checkmark_button))

        storyName = Workspace.activeStory.title

        setToolbar()

        closeKeyboardOnTouch(rootView)
        transcriptionText = rootView?.findViewById(R.id.transcription)
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsKeyString = storyName + slideNumber + TRANSCRIPTION_TEXT
        val savedTranscriptionText = prefs.getString(prefsKeyString, "")
        transcriptionText!!.setText(savedTranscriptionText)
        transcriptionText!!.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, aft: Int) {}
            override fun afterTextChanged(s: Editable) {
                addTranscription()
            }
        })
        transcriptionText!!.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                addTranscription()
            }
        }
        return rootView
    }

    override fun setToolbar() {
        val bundle = Bundle()
        bundle.putBooleanArray("buttonEnabled", booleanArrayOf(true,true,true,true))
        bundle.putInt("slideNum", slideNum)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().add(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()

        recordingToolbar.keepToolbarVisible()
        recordingToolbar.stopToolbarMedia()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        val item = menu!!.getItem(0)
        super.onCreateOptionsMenu(menu, inflater)
        item.setIcon(R.drawable.ic_headset_mic_white_48dp)
    }

    override fun onStart() {
        super.onStart()
        //dramatize phase not unlocked yet
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsKeyString = storyName!! + IS_R_CONSULTANT_APPROVED
        if (!prefs.getBoolean(prefsKeyString, false)) {
            //TODO: remove call to create story here and make global var that stores whether story has been created in DB
            //TODO: put all the volley functions into a separate class? (have redundancy between some classes)
            //requestRemoteReview(context, FileSystem.getContentSlideAmount(storyName))
            getSlidesStatus()
            setCheckmarkButton(rootView!!.findViewById(R.id.fragment_backtranslation_r_concheck_checkmark_button))
            phaseUnlocked = checkAllMarked()
            if (phaseUnlocked) {
                unlockDramatizationPhase()
            }
        }
    }

    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on pause.
     */
    override fun onPause() {
        super.onPause()
        recordingToolbar.onPause()
        closeKeyboard(rootView)
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
            val imm = context!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(viewToFocus.windowToken, 0)
            viewToFocus.requestFocus()
        }
    }

    //save remote consultant approval
    private fun saveConsultantApproval() {
        val prefsEditor = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE).edit()
        prefsEditor.putBoolean(storyName!! + IS_R_CONSULTANT_APPROVED, true)
        prefsEditor.apply()
    }


    //initializes the checkmark button
    private fun setCheckmarkButton(button: ImageButton) {
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsKeyString = storyName + slideNumber + IS_CHECKED
        val isChecked = prefs.getInt(prefsKeyString, 0)
        when (isChecked) {
            1 ->
                button.background = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_green, null)
            -1 ->
                button.background = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_gray, null)
            else -> button.background = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_yellow, null)
        }
    }

    //TODO: check to see if all slides have been approved.
    private fun checkAllMarked(): Boolean {
        //FIXME
        var marked: Int
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val numStorySlides = Workspace.activeStory.slides.count()
        for (i in 0 until numStorySlides) {
            marked = prefs.getInt(storyName + i + IS_CHECKED, 0)
            if (marked != 1) {
                return false
            }
        }
        return true
    }

    private fun unlockDramatizationPhase() {
        //FIXME
        Toast.makeText(context, "Congrats!", Toast.LENGTH_SHORT).show()
        saveConsultantApproval()
        val dramatizationPhaseIndex = 6
        val phases = Workspace.phases
        Workspace.activePhase = phases[dramatizationPhaseIndex]
        val intent = Intent(context, Workspace.activePhase.getTheClass())
        intent.putExtra(SLIDE_NUM, 0)
        activity!!.startActivity(intent)
    }

    private fun getSlidesStatus() {
        //FIXME
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsEditor = prefs.edit()
                val phone_id = Settings.Secure.getString(context!!.contentResolver,
                        Settings.Secure.ANDROID_ID)
                js = HashMap()
                js!!["Key"] = getString(R.string.api_token)
                js!!["PhoneId"] = phone_id
                js!!["TemplateTitle"] = Workspace.activeStory.title
                val req = object : StringRequest(Request.Method.POST, getString(R.string.url_get_slide_status), Response.Listener { response ->
                    try {
                        obj = JSONObject(response)
                    } catch (e: JSONException) {
                        e.printStackTrace()
            }

            var arr: JSONArray? = null
            try {
                arr = obj!!.getJSONArray("Status")
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            //int[] arr = {-1,1,0,1,1,1};
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    //-1 not approved, 0 pending, 1 approved
                    try {
                        prefsEditor.putInt(storyName + i + IS_CHECKED, arr.getInt(i))
                        prefsEditor.apply()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                }
            }
            Log.i("LOG_VOLEY", response)
            resp = response
        }, Response.ErrorListener { error ->
            Log.e("LOG_VOLLEY_ERR_STATUS", error.toString())
            Log.e("LOG_VOLLEY", "HIT ERROR")
            //testErr = error.toString();
        }) {
            @Throws(AuthFailureError::class)
            public override fun getParams(): Map<String, String>? {
                return js
            }
        }
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(req)
    }

    fun requestRemoteReview(con: Context?, numSlides: Int) {
        val phone_id = Settings.Secure.getString(con!!.contentResolver,
                Settings.Secure.ANDROID_ID)
        js = HashMap()
        js!!["Key"] = getString(R.string.api_token)
        js!!["PhoneId"] = phone_id
        js!!["TemplateTitle"] = Workspace.activeStory.title
        js!!["NumberOfSlides"] = Integer.toString(numSlides)
        val req = object : StringRequest(Request.Method.POST, getString(R.string.url_request_review), Response.Listener { response ->
            Log.i("LOG_VOLLEY_RESP_RR", response)
            resp = response
        }, Response.ErrorListener { error ->
            Log.e("LOG_VOLLEY_ERR_RR", error.toString())
            Log.e("LOG_VOLLEY", "HIT ERROR")
            //testErr = error.toString();
        }) {
            override fun getParams(): Map<String, String>? {
                return js
            }
        }
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(req)
    }

    private fun addTranscription() {
        val transcript = transcriptionText?.text.toString()
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        prefsEditor.putString(storyName + slideNumber + TRANSCRIPTION_TEXT, transcript)
        prefsEditor.apply()
    }

    companion object {
        val SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG"
        val R_CONSULTANT_PREFS = "Consultant_Checks"
        val IS_R_CONSULTANT_APPROVED = "isApproved"
        private val IS_CHECKED = "isCheckedi"
        val TRANSCRIPTION_TEXT = "TranscriptionText"
    }
}
