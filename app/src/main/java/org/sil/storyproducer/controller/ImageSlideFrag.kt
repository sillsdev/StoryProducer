package org.sil.storyproducer.controller

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.draft.DraftFrag
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.getStoryImage
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer

class ImageSlideFrag : Fragment() {

    private var referncePlayButton: ImageButton? = null
    private var slideNum: Int = 0 //gets overwritten
    private var rootView: View? = null
    private var listener : OnAudioPlayListener? = null

    interface OnAudioPlayListener {
        fun onPlayButtonClicked(path: String, image : ImageButton, stopImage: Int, playImage : Int)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slideNum = parentFragment?.arguments?.getInt(SlidePhaseFrag.SLIDE_NUM) ?: 0
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_image_slide, container, false)
        setPic(rootView?.findViewById(R.id.fragment_image_view))
        return rootView
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context is ImageSlideFrag.OnAudioPlayListener) {
            listener = context
        }
    }

    private fun setPic(slideImage: ImageView?) {
        val slidePicture: Bitmap? = getStoryImage(activity!!,slideNum)

        slideImage?.setImageBitmap(slidePicture)

        //Set up the reference audio and slide number overlays
        referncePlayButton = rootView?.findViewById(R.id.fragment_reference_audio_button)
        setReferenceAudioButton()

        rootView?.findViewById<TextView>(R.id.slide_number_text)?.text = slideNum.toString()
    }

    private fun setReferenceAudioButton() {
        referncePlayButton?.setOnClickListener {
            //if file not found, alert!
            if (!storyRelPathExists(context!!, Workspace.activePhase.getReferenceAudioFile(slideNum))) {
                //TODO make "no audio" string work for all phases
                Snackbar.make(rootView!!, R.string.draft_playback_no_lwc_audio, Snackbar.LENGTH_SHORT).show()
            }
            else {
                listener?.onPlayButtonClicked(Workspace.activePhase.getReferenceAudioFile(slideNum), referncePlayButton!!, R.drawable.ic_stop_white_36dp, R.drawable.ic_play_arrow_white_36dp)

                Toast.makeText(context, R.string.draft_playback_lwc_audio, Toast.LENGTH_SHORT).show()
                when(Workspace.activePhase.phaseType){
                    PhaseType.DRAFT -> saveLog(activity!!.getString(R.string.LWC_PLAYBACK))
                    PhaseType.COMMUNITY_CHECK -> saveLog(activity!!.getString(R.string.DRAFT_PLAYBACK))
                    else -> {}
                }
            }
        }
    }
}
