package org.sil.storyproducer.model.messaging

import java.sql.Timestamp

/**
 * Created by annmcostantino on 4/7/2018.
 */

class Approval(
    val slideNumber: Int,
    val storyId: Int,
    val timeSent: Timestamp,
    val approvalStatus: Boolean
)
