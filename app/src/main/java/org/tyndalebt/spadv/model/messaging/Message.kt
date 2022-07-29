package org.tyndalebt.spadv.model.messaging

import java.sql.Timestamp

/**
 * Created by annmcostantino on 4/7/2018.
 */
class Message(val isFromPhone: Boolean,
              val message: String
)

class MessageROCC(val slideNumber: Int,
    val storyId: Int,
    val isConsultant: Boolean,
    val isTranscript: Boolean,
    val timeSent: Timestamp,
    val message: String
)
