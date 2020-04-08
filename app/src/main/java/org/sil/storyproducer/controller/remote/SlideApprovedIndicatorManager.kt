package org.sil.storyproducer.controller.remote

import android.content.Context
import android.support.graphics.drawable.VectorDrawableCompat
import android.view.*
import android.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.messaging.Approval

class ApprovalIndicatorManager(
    val context: Context,
    val scope: CoroutineScope,
    val approvedIndicator: ImageButton,
    val slide: Slide,
    val slideNumber: Int?
){
    private var greenCheckmark: VectorDrawableCompat
    private var grayCheckmark: VectorDrawableCompat
    private var approvalReceiveChannel: ReceiveChannel<Approval>? = null

    init {
        greenCheckmark = VectorDrawableCompat.create(context.resources, R.drawable.ic_checkmark_green, null)!!
        grayCheckmark = VectorDrawableCompat.create(context.resources, R.drawable.ic_checkmark_gray, null)!!

        approvedIndicator.background = if (slide.isApproved) {
            greenCheckmark
        } else {
            grayCheckmark
        }
    }

    public fun start() {
        approvedIndicator.background = if (slide.isApproved) {
            greenCheckmark
        } else {
            grayCheckmark
        }

        approvalReceiveChannel = Workspace.approvalChannel.openSubscription()
        scope.launch(Dispatchers.Main) {
            for (approval in approvalReceiveChannel!!) {
                if (approval.slideNumber == slideNumber && approval.storyId == Workspace.activeStory.remoteId) {
                    approvedIndicator.background = if (approval.approvalStatus) { greenCheckmark } else { grayCheckmark }
                }
            }
        }
    }

    public fun stop() {
        approvalReceiveChannel?.cancel()
        approvalReceiveChannel = null
    }
}
