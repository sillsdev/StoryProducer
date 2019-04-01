package org.sil.storyproducer.controller.remote

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.MessageAdapter
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.messaging.Message
import org.sil.storyproducer.tools.Network.VolleySingleton
import org.sil.storyproducer.tools.Network.paramStringRequest
import java.util.*

/**
 * Created by annmcostantino on 2/19/2018.
 */

class RemoteCheckFrag : Fragment() {

    private var slideNumber: Int = 0
    private lateinit var storyName: String
    private lateinit var messageSent: EditText

    private var obj: JSONObject? = null
    private var resp: String? = null
    private var js: MutableMap<String, String>? = null

    private var msgAdapter: MessageAdapter? = null
    private var messagesView: ListView? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        val passedArgs = this.arguments
        slideNumber = passedArgs!!.getInt(SLIDE_NUM)
        storyName = Workspace.activeStory.title
        setHasOptionsMenu(true)

        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        val phoneID = Settings.Secure.getString(context!!.contentResolver,
                Settings.Secure.ANDROID_ID)
        prefsEditor.putString("PhoneId", phoneID).apply()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_remote_check_layout, container, false)

        val messageTitle = rootView!!.findViewById<TextView>(R.id.messaging_title)
        messageTitle.append("Messages for Slide $slideNumber")

        messagesView = rootView.findViewById(R.id.message_history)

        val sendMessageButton = rootView.findViewById<Button>(R.id.button_send_msg)
        sendMessageButton.setOnClickListener { sendMessage() }

        //grab old adapter or make a new one
        msgAdapter = loadSharedPreferenceMessageHistory()
        messagesView!!.adapter = msgAdapter

        //set texts for this view
        messageSent = rootView.findViewById(R.id.sendMessage)
        messageSent.setHint(R.string.message_hint)
        messageSent.setHintTextColor(ContextCompat.getColor(context!!, R.color.black))
        //load saved message draft and load saved message adapter
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        messageSent.setText(prefs.getString(storyName + slideNumber + TO_SEND_MESSAGE, ""))

        getMessages()
        if (msgAdapter!!.count > 0) {
            messagesView!!.setSelection(msgAdapter!!.count)
        }

        closeKeyboardOnTouch(rootView)
        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        val item = menu!!.getItem(0)
        super.onCreateOptionsMenu(menu, inflater)
        item.setIcon(R.drawable.ic_message_white_24dp)
    }

    /**
     * This function serves to save data when leaving phase or slide.
     */
    override fun onPause() {
        super.onPause()

        //save message draft
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, messageSent.text.toString()).apply()

        //save message adapter as well
        saveSharedPreferenceMessageHistory()
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

    //loads the (local) message history or creates a new one if it doesn't yet exist
    private fun loadSharedPreferenceMessageHistory(): MessageAdapter {
        val msgAdapter = MessageAdapter(context)
        val msgs: List<Message>
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val gsonBuilder = GsonBuilder()
        val gson = gsonBuilder.create()
        val json = prefs.getString(R_MESSAGE_HISTORY + storyName + slideNumber, "")
        val lastID = prefs.getInt(R_LAST_ID + storyName + slideNumber, -1)
        if (!json!!.isEmpty()) {
            val type = object : TypeToken<List<Message>>() {

            }.type
            msgs = gson.fromJson(json, type)
            msgAdapter.messageHistory = msgs
            msgAdapter.lastID = lastID

        }
        return msgAdapter
    }

    //saves the (local) message history
    private fun saveSharedPreferenceMessageHistory() {
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        val gsonBuilder = GsonBuilder()
        val gson = gsonBuilder.create()
        val json = gson.toJson(msgAdapter!!.messageHistory)
        val lastID = msgAdapter!!.lastID
        prefsEditor.putString(R_MESSAGE_HISTORY + storyName + slideNumber, json).apply()
        prefsEditor.putInt(R_LAST_ID + storyName + slideNumber, lastID).apply()
    }

    //function to send messages to remote consultant the given slide
    private fun sendMessage() {
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        val phoneID = Settings.Secure.getString(context!!.contentResolver,
                Settings.Secure.ANDROID_ID)
        js = HashMap()
        js!!["Key"] = getString(R.string.api_token)
        js!!["PhoneId"] = phoneID
        js!!["StoryTitle"]  = Workspace.activeStory.title
        js!!["SlideNumber"] = Integer.toString(slideNumber)
        val message = messageSent.text.toString()
        js!!["Message"] = message

        val req = object : paramStringRequest(Request.Method.POST, getString(R.string.url_send_message), js, Response.Listener { response ->
            Log.i("LOG_VOLLEY_MSG", response.toString())
            resp = response

            try {
                obj = JSONObject(response)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            try {
                resp = obj!!.getString("Success")

            } catch (e: JSONException) {
                e.printStackTrace()
            }

            if (resp != null) {
                if (resp === "true") {
                    //set text back to blank
                    prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, "")
                    prefsEditor.apply()
                    messageSent.setText("")
                    Toast.makeText(activity!!.applicationContext, R.string.remote_check_msg_sent, Toast.LENGTH_SHORT).show()

                    //pull new messages from the server
                    getMessages()
                } else {
                    Toast.makeText(activity!!.applicationContext, R.string.remote_check_msg_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }, Response.ErrorListener { error ->
            Log.e("LOG_VOLLEY_MSG_ERR", error.toString())
            Log.e("LOG_VOLLEY", "HIT ERROR")
            //Save the message to send next time
            prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, messageSent.text.toString())
            prefsEditor.apply()

            if (error is NoConnectionError || error is NetworkError
                    || error is AuthFailureError) {
                Toast.makeText(activity!!.applicationContext, R.string.remote_check_msg_no_connection, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity!!.applicationContext, R.string.remote_check_msg_failed, Toast.LENGTH_SHORT).show()
            }
        }) {
            override fun getParams(): Map<String, String> {
                return this.mParams
            }
        }
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(req)
    }

    private fun getMessages() {
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val phoneID = prefs.getString(getString(R.string.PhoneId), "")

        js = HashMap()
        js!!["Key"] = getString(R.string.api_token)
        js!!["PhoneId"] = phoneID!!
        js!!["StoryTitle"] = Workspace.activeStory.title
        js!!["SlideNumber"] = Integer.toString(slideNumber)
        js!!["LastId"] = Integer.toString(msgAdapter!!.lastID)

        val req = object : StringRequest(Request.Method.POST, getString(R.string.url_get_messages), Response.Listener { response ->
            //returns messages array IsTranslator: boolean + Message: String
            //returns LastId: Integer
            try {
                obj = JSONObject(response)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            var msgs: JSONArray? = null

            try {
                msgs = obj!!.getJSONArray("Messages")
                val id = obj!!.getInt("LastId")
                if (id != -1) {
                    msgAdapter!!.lastID = id
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            }

            //get all msgs and store into shared preferences
            if (msgs != null) {
                for (j in 0 until msgs.length()) {

                    try {
                        val currMsg = msgs.getJSONObject(j)
                        val num = currMsg.getInt("IsTranslator")
                        val isFromTranslator = num == 1
                        val msg = currMsg.getString("Message")
                        val m = Message(isFromTranslator, msg)
                        msgAdapter!!.add(m)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }

                if (messagesView != null) {
                    if (msgAdapter!!.count > 0) {
                        messagesView!!.setSelection(msgAdapter!!.count)
                    }
                }
            }

            Log.i("LOG_VOLLEY", response.toString())

            resp = response
        }, Response.ErrorListener { error ->
            Log.e("LOG_VOLLEY", error.toString())
            Log.e("LOG_VOLLEY", "HIT ERROR IN RECEIVE MSG")
            //testErr = error.toString();
        }) {
            @Throws(AuthFailureError::class)
            public override fun getParams(): Map<String, String>? {

                return js
            }
        }
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(req)
    }

    companion object {
        val SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG"
        val R_CONSULTANT_PREFS = "Consultant_Checks"
        private val TO_SEND_MESSAGE = "SND_MSG"
        private val R_MESSAGE_HISTORY = "Message History"
        private val R_LAST_ID = "Last Int"
    }
}
