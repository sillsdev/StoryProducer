package org.sil.storyproducer.controller.remote

import android.content.Context
import android.support.graphics.drawable.VectorDrawableCompat
import android.view.*
import android.widget.*
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.SlideType
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

                // Approve story if all slides in the story have been approved.
                var allApproved = true
                var story = Workspace.activeStory
                for (slide in story.slides) {
                    if ((slide.slideType == SlideType.FRONTCOVER ||
                         slide.slideType == SlideType.NUMBEREDPAGE ||
                         slide.slideType == SlideType.LOCALSONG) && 
                        !slide.isApproved) {

                        allApproved = false
                    }
                }
                story.isApproved = allApproved
            }
        }
    }

    public fun stop() {
        approvalReceiveChannel?.cancel()
    }
}
