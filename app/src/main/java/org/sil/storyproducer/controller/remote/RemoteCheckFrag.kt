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
import com.android.volley.AuthFailureError
import com.android.volley.NetworkError
import com.android.volley.NoConnectionError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONException
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.MessageAdapter
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.messaging.Message
import org.sil.storyproducer.tools.Network.VolleySingleton
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set

/**
 * Created by annmcostantino on 2/19/2018.
 */

class RemoteCheckFrag : Fragment() {

    private lateinit var rootView: View
    private val storyName: String? = null
    private lateinit var messageTitle: TextView
    private lateinit var sendMessageButton: Button
    private lateinit var messageSent: EditText
    private var slideNumber: Int = 0

    private var resp: String? = null

    private lateinit var msgAdapter: MessageAdapter
    private lateinit var messagesView: ListView

    private var successToast: Toast? = null
    private var noConnection: Toast? = null
    private var unknownError: Toast? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setHasOptionsMenu(true)
        successToast = Toast.makeText(activity!!.applicationContext, R.string.remote_check_msg_sent, Toast.LENGTH_SHORT)
        noConnection = Toast.makeText(activity!!.applicationContext, R.string.remote_check_msg_no_connection, Toast.LENGTH_SHORT)
        unknownError = Toast.makeText(activity!!.applicationContext, R.string.remote_check_msg_failed, Toast.LENGTH_SHORT)

        //these lines should be put somewhere else (one time only)
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        val phoneId = Settings.Secure.getString(context!!.contentResolver,
                Settings.Secure.ANDROID_ID)
        prefsEditor.putString("PhoneId", phoneId).apply()

        slideNumber = this.arguments!!.getInt(SLIDE_NUM)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_remote_check_layout, container, false)

        messageTitle = rootView.findViewById<View>(R.id.messaging_title) as TextView
        messagesView = rootView.findViewById<View>(R.id.message_history) as ListView

        sendMessageButton = rootView.findViewById<View>(R.id.button_send_msg) as Button
        messageSent = rootView.findViewById<View>(R.id.sendMessage) as EditText

        closeKeyboardOnTouch(rootView)

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater?) {
        val item = menu.getItem(0)
        super.onCreateOptionsMenu(menu, inflater)
        item.setIcon(R.drawable.ic_message_white_24dp)
    }

    override fun onStart() {
        super.onStart()

        //grab old adapter or make a new one
        msgAdapter = loadSharedPreferenceMessageHistory()
        messagesView.adapter = msgAdapter

        //set texts for this view
        val titleString = " $slideNumber"
        messageTitle.append(titleString)
        messageSent.setHint(R.string.message_hint)
        messageSent.setHintTextColor(ContextCompat.getColor(context!!, R.color.black))
        //load saved message draft and load saved message adapter
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        messageSent.setText(prefs.getString(storyName + slideNumber + TO_SEND_MESSAGE, ""))
        getMessages()
        if (msgAdapter.count > 0) {
            messagesView.setSelection(msgAdapter.count)
        }
        sendMessageButton.setOnClickListener { sendMessage() }
    }

    /**
     * This function serves to save data when leaving phase or slide.
     */
    override fun onPause() {
        super.onPause()
        closeKeyboard(rootView)

        //save message draft
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, messageSent.text.toString()).apply()

        //save message adapter as well
        saveSharedPreferenceMessageHistory()
    }

    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on stop.
     */
    override fun onStop() {
        super.onStop()
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

    //loads the (local) message history or creates a new one if it doesn't yet exist
    private fun loadSharedPreferenceMessageHistory(): MessageAdapter {
        msgAdapter = MessageAdapter(context)
        val msgs: List<Message>
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val gsonBuilder = GsonBuilder()
        val gson = gsonBuilder.create()
        val json = prefs.getString(R_MESSAGE_HISTORY + storyName + slideNumber, "")
        val lastID = prefs.getInt(R_LAST_ID + storyName + slideNumber, -1)
        if (json!!.isNotEmpty()) {
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
        val json = gson.toJson(msgAdapter.messageHistory)
        val lastID = msgAdapter.lastID
        prefsEditor.putString(R_MESSAGE_HISTORY + storyName + slideNumber, json).apply()
        prefsEditor.putInt(R_LAST_ID + storyName + slideNumber, lastID).apply()
    }

    //function to send messages to remote consultant the given slide
    private fun sendMessage() {

        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        val js = HashMap<String, String>()
        val message = messageSent.text.toString()
        js["IsTranscript"] = 0.toString()
        sendSlideSpecificRequest(context!!, slideNumber, getString(R.string.url_send_message), message, {
            prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, "")
            prefsEditor.apply()
            messageSent.setText("")
            successToast!!.show()
            getMessages()
        }, {
            //Save the message to send next time
            prefsEditor.putString(storyName + slideNumber + TO_SEND_MESSAGE, messageSent.text.toString())
            prefsEditor.apply()

            if (it is NoConnectionError || it is NetworkError || it is AuthFailureError) {
                noConnection!!.show()
            } else {
                unknownError!!.show()
            }

        }, js)
    }

    private fun getMessages() {
        sendProjectSpecificRequest(context!!, getString(R.string.url_get_messages), {
            val messages = it.getJSONArray("Messages")
            val messageList = ArrayList<Message>()
            for (j in 0 until messages.length()) {
                val message = messages.getJSONObject(j)
                if (message.getInt("slideNumber") == slideNumber) {
                    val isConsultant = message.getInt("isConsultant") == 1
                    val isTranscript = message.getInt("isTranscript") == 1
                    val text = message.getString("text")
                    val m = Message(isConsultant, isTranscript, text)
                    messageList.add(m)
                }
            }
            msgAdapter.messageHistory = messageList

            val approvals = it.getJSONArray("Approvals")
            for (j in 0 until approvals.length()) {
                val approval = approvals.getJSONObject(j)
                val storyId = approval.getInt("storyId")
                val slideNumber = approval.getInt("slideNumber")
                val isApproved = approval.getInt("isApproved") == 1

                for (story in Workspace.Stories) {
                    if (story.remoteId == storyId) {
                      Log.e("@pwhite", "setting approval to $isApproved for story ${story.remoteId} and slide $slideNumber")
                        story.slides[slideNumber].isApproved = isApproved
                    }
                }
            }

            if (msgAdapter.count > 0) {
                messagesView.setSelection(msgAdapter.count)
            }
        }, {})
    }

    companion object {

        const val R_CONSULTANT_PREFS = "Consultant_Checks"
        const val TO_SEND_MESSAGE = "SND_MSG"
        private const val R_MESSAGE_HISTORY = "Message History"
        private const val R_LAST_ID = "Last Int"
    }

}
