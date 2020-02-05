package org.sil.storyproducer.model

import android.util.Log
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import org.sil.storyproducer.model.messaging.Message
import java.net.URI
import java.nio.ByteBuffer

class MessageWebSocketClient(uri: URI) : WebSocketClient(uri) {

    private lateinit var messageReceiveChannel: ReceiveChannel<Message>

    override fun onOpen(handshakeData: ServerHandshake) {
        Log.e("@pwhite", "opened web-socket; starting queue listener")
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
    }

    override fun onMessage(message: ByteBuffer) {
    }

    override fun onMessage(messageString: String?) {
        Log.e("@pwhite", "got message: ${messageString!!}")
        val message = JSONObject(messageString)
        val slideNumber = message.getInt("slideNumber")
        val storyId = message.getInt("storyId")
        val isConsultant = message.getBoolean("isConsultant")
        val isTranscript = message.getBoolean("isTranscript")
        val text = message.getString("text")
        GlobalScope.launch {
            Workspace.messageChannel.send(Message(slideNumber, storyId, isConsultant, isTranscript, text))
        }
    }

    override fun onError(ex: Exception) {
    }
}
