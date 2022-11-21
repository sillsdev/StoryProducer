package org.tyndalebt.storyproduceradv.controller.remote

import android.content.Context
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import android.view.*
import android.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.model.Slide
import org.tyndalebt.storyproduceradv.model.SlideType
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.model.messaging.Approval
import org.tyndalebt.storyproduceradv.tools.file.workspaceRelPathExists

class ApprovalIndicatorManager(
    val context: Context,
    val scope: CoroutineScope,
    private val approvedIndicator: ImageButton,
    val slide: Slide,
    val slideNumber: Int?
){
    private var greenCheckmark: VectorDrawableCompat
    private var grayCheckmark: VectorDrawableCompat
    private var approvalReceiveChannel: ReceiveChannel<Approval>? = null

    init {
        greenCheckmark = VectorDrawableCompat.create(context.resources, R.drawable.ic_checkmark_green, null)!!
        grayCheckmark = VectorDrawableCompat.create(context.resources, R.drawable.ic_checkmark_gray, null)!!
    }

    public fun start() {
        // TODO @pwhite: Make only one instance of these run at a time, so make
        // sure start and stop happen upon visibility changes.

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
                Workspace.processStoryApproval()
            }
        }
    }

    public fun stop() {
        approvalReceiveChannel?.cancel()
    }
}
