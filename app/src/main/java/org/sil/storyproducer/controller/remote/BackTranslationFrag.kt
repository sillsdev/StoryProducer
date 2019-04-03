package org.sil.storyproducer.controller.remote

import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.consultant.ConsultantBaseFrag
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.Network.BackTranslationUpload.testErr
import org.sil.storyproducer.tools.Network.VolleySingleton
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import java.util.*

/**
 * Created by alexamhepner on 10/23/17.
 */

class BackTranslationFrag : ConsultantBaseFrag(), RecordingToolbar.RecordingListener {

    private var storyName: String? = null
    private var phaseUnlocked = false

    private var transcriptionText: EditText? = null

    private var obj: JSONObject? = null
    private var resp: String? = null
    private var js: MutableMap<String, String>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_backtranslation, container, false)
        storyName = Workspace.activeStory.title

        setPic(rootView!!.findViewById(R.id.fragment_backtranslation_image_view))
        setCheckmarkButton(rootView!!.findViewById(R.id.fragment_backtranslation_r_concheck_checkmark_button))

        setToolbar()

        closeKeyboardOnTouch(rootView)

        transcriptionText = rootView?.findViewById(R.id.transcription)
        transcriptionText?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, aft: Int) {}
            override fun afterTextChanged(s: Editable) {
                addTranscription()
            }
        })
        transcriptionText?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
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
        if(!Workspace.activeStory.isApproved) {
            requestRemoteReview()
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

    private fun unlockDramatizationPhase() {
        Toast.makeText(context, "Congrats!", Toast.LENGTH_SHORT).show()
        saveConsultantApproval()
        launchDramatizationPhase()
    }

    private fun getSlidesStatus() {
        val phoneID = Settings.Secure.getString(context!!.contentResolver,
                Settings.Secure.ANDROID_ID)
        js = HashMap()
        js!!["Key"] = getString(R.string.api_token)
        js!!["PhoneId"] = phoneID
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
                        if(arr.getInt(i) == 1) {
                            Workspace.activeStory.slides[i].isChecked = true
                        }
                        else if(arr.getInt(i) == -1){
                            Workspace.activeStory.slides[i].isChecked = false
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
            Log.i("LOG_VOLLEY", response)
            resp = response
        }, Response.ErrorListener { error ->
            Log.e("LOG_VOLLEY_ERR_STATUS", error.toString())
            Log.e("LOG_VOLLEY", "HIT ERROR")
            //testErr = error.toString();
        }) {
            public override fun getParams(): Map<String, String>? {
                return js
            }
        }
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(req)
    }

    private fun requestRemoteReview() {
        val phoneID = Settings.Secure.getString(context?.contentResolver,
                Settings.Secure.ANDROID_ID)
        js = HashMap()
        js!!["Key"] = getString(R.string.api_token)
        js!!["PhoneId"] = phoneID
        js!!["TemplateTitle"] = Workspace.activeStory.title
        js!!["NumberOfSlides"] = Integer.toString(Workspace.activeStory.slides.count())
        val req = object : StringRequest(Request.Method.POST, getString(R.string.url_request_review), Response.Listener { response ->
            Log.i("LOG_VOLLEY_RESP_RR", response)
            resp = response
        }, Response.ErrorListener { error ->
            Log.e("LOG_VOLLEY_ERR_RR", error.toString())
            Log.e("LOG_VOLLEY", "HIT ERROR")
            testErr = error.toString()
        }) {
            override fun getParams(): Map<String, String>? {
                return js
            }
        }
        VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(req)
    }

    private fun addTranscription() {
        val transcript = transcriptionText?.text.toString()
        Workspace.activeStory.slides[slideNum].remoteTranscription = transcript
    }
}
