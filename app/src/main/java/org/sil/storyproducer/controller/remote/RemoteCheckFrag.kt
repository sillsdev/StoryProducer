package org.sil.storyproducer.controller.remote

import android.graphics.Canvas
import org.sil.storyproducer.model.PhaseType
import android.graphics.Color
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.file.genDefaultImage
import org.sil.storyproducer.tools.file.getStoryImage
import android.graphics.Bitmap
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.sql.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.json.JSONObject
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.MessageAdapter
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.controller.SlidePhaseFrag
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.UploadState
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.messaging.Approval
import org.sil.storyproducer.model.messaging.Message
import kotlin.math.max

/**
 * Created by annmcostantino on 2/19/2018.
 */

class RemoteCheckFrag : SlidePhaseFrag(), CoroutineScope by MainScope() {

    private val storyName: String? = null
    private lateinit var sendMessageButton: Button
    private lateinit var messageSent: EditText
    private lateinit var uploadAudioButtonManager: UploadAudioButtonManager
    private lateinit var approvalIndicatorManager: ApprovalIndicatorManager

    private var resp: String? = null

    private lateinit var msgAdapter: MessageAdapter
    private lateinit var messagesView: ListView

    private lateinit var successToast: Toast
    private lateinit var noConnection: Toast
    private lateinit var unknownError: Toast

    private var messageReceiveChannel: ReceiveChannel<Message>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        successToast = Toast.makeText(activity!!.applicationContext, R.string.remote_check_msg_sent, Toast.LENGTH_SHORT)
        noConnection = Toast.makeText(activity!!.applicationContext, R.string.remote_check_msg_no_connection, Toast.LENGTH_SHORT)
        unknownError = Toast.makeText(activity!!.applicationContext, R.string.remote_check_msg_failed, Toast.LENGTH_SHORT)

        //these lines should be put somewhere else (one time only)
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        val phoneId = Settings.Secure.getString(context!!.contentResolver,
                Settings.Secure.ANDROID_ID)
        prefsEditor.putString("PhoneId", phoneId).apply()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_remote_check_layout, container, false)
        
        initializeViews()

        messagesView = rootView.findViewById<View>(R.id.message_history) as ListView

        sendMessageButton = rootView.findViewById<View>(R.id.button_send_msg) as Button
        messageSent = rootView.findViewById<View>(R.id.sendMessage) as EditText

        uploadAudioButtonManager = UploadAudioButtonManager(
            context!!,
            rootView.findViewById(R.id.upload_audio_botton),
            { slide.backTranslationUploadState },
            { slide.backTranslationUploadState = it },
            { slide.backTranslationRecordings.selectedFile }, 
            slideNumber)

        approvalIndicatorManager = ApprovalIndicatorManager(
            context!!,
            this,
            rootView.findViewById(R.id.slide_approved_indicator),
            slide,
            slideNumber)

        rootView.setOnClickListener { closeKeyboard(rootView) }

        return rootView
    }

    override fun setPic() {
        val slideImage = rootView.findViewById<ImageView>(R.id.fragment_image_view)
        var slidePicture: Bitmap = getStoryImage(context!!, slideNumber, 2) ?: genDefaultImage()

        if (slideNumber < Workspace.activeStory.slides.size) {
            //scale down image to not crash phone from memory error from displaying too large an image
            //Get the height of the phone.
            val scalingFactor = 0.4
            val height = (context!!.resources.displayMetrics.heightPixels * scalingFactor * 0.3).toInt()
            val width = (context!!.resources.displayMetrics.widthPixels * 0.3).toInt()

            slidePicture = BitmapScaler.centerCrop(slidePicture, height, width)
            slidePicture = slidePicture.copy(Bitmap.Config.RGB_565, true)
            val canvas = Canvas(slidePicture)
            //only show the untranslated title in the Learn phase.
            val tOverlay = Workspace.activeStory.slides[slideNumber]
                    .getOverlayText(false, Workspace.activeStory.lastPhaseType == PhaseType.LEARN)
            //if overlay is null, it will not write the text.
            tOverlay?.setPadding(max(20, 20 + (canvas.width - width) / 2))
            tOverlay?.draw(canvas)
        }
        //Set the height of the image view
        slideImage.requestLayout()

        slideImage.setImageBitmap(slidePicture)
    }

    override fun onStart() {
        super.onStart()

        //grab old adapter or make a new one
        msgAdapter = MessageAdapter(context!!)
        messagesView.adapter = msgAdapter
        for (message in Workspace.messages) {
            if (message.slideNumber == slideNumber && message.storyId == Workspace.activeStory.remoteId) {
                msgAdapter.add(message)
            }
        }

        // There is a small race condition here. If a message is sent after we
        // load all the messages from the workspace, but before we open the
        // subscription, then it will not be displayed. However, this is not
        // too significant because it will appear next time the fragment is
        // recreated.
        messageReceiveChannel = Workspace.messageChannel.openSubscription()
        launch(Dispatchers.Main) {
            for (message in messageReceiveChannel!!) {
                Log.e("@pwhite", "got message!")
                msgAdapter.setQueuedMessages(Workspace.queuedMessages)
                if (message.slideNumber == slideNumber && message.storyId == Workspace.activeStory.remoteId) {
                    msgAdapter.add(message)
                }
                messagesView.setSelection(msgAdapter.messageHistory.size - 1)
            }
        }

        approvalIndicatorManager.start()

        //load saved message draft and load saved message adapter
        val prefs = activity!!.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        messageSent.setText(prefs.getString(storyName + slideNumber + TO_SEND_MESSAGE, ""))
        
        val whiteSendIcon = VectorDrawableCompat.create(resources, R.drawable.ic_send_white_24dp, null)!!
        val blackSendIcon = VectorDrawableCompat.create(resources, R.drawable.ic_send_black_24dp, null)!!
        sendMessageButton.setOnClickListener {
            val messageText = messageSent.text.toString()
            if (messageText.length > 0) {
                val storyId = Workspace.activeStory.remoteId ?: 0
                val message = Message(slideNumber, storyId, false, false, Timestamp(0), messageText)
                msgAdapter.addQueuedMessage(message)
                launch {
                    Workspace.toSendMessageChannel.send(message)
                }
                messageSent.setText("")
                sendMessageButton.background = blackSendIcon;
            }
        }
        
        messageSent.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
                sendMessageButton.background = whiteSendIcon;
            }
        })
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if (::uploadAudioButtonManager.isInitialized) {
            uploadAudioButtonManager.refreshBackground()
        }
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

    override fun onStop() {
        super.onStop()

        approvalIndicatorManager.stop()

        messageReceiveChannel?.cancel()
        messageReceiveChannel = null
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

    private fun messageIsRelevant(m: Message) =
            m.storyId == Workspace.activeStory.remoteId && m.slideNumber == slideNumber

    companion object {

        const val R_CONSULTANT_PREFS = "Consultant_Checks"
        const val TO_SEND_MESSAGE = "SND_MSG"
        private const val R_MESSAGE_HISTORY = "Message History"
        private const val R_LAST_ID = "Last Int"
    }

}
