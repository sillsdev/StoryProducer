package org.sil.storyproducer.controller.adapter

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace

open class RecordingAdapterV2(private val values: MutableList<String>?) : RecyclerView.Adapter<RecordingAdapterV2.ViewHolder>() {

    var onItemClick: ((String) -> Unit)? = null
    var onItemLongClick: ((String) -> Unit)? = null
    var onPlayClick: ((String, ImageButton) -> Unit)? = null
    var onDeleteClick: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater : LayoutInflater = LayoutInflater.from(parent.context)
        val individualAudio = inflater.inflate(R.layout.audio_comment_list_item, parent, false)

        return ViewHolder(individualAudio)
    }

    override fun getItemCount(): Int = values?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val audioText = values?.get(position)
        if (audioText != null) {
            holder.bindView(audioText)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            itemView.setOnClickListener{
                onItemClick?.invoke(values?.get(adapterPosition).toString())
            }
            itemView.setOnLongClickListener {
                onItemLongClick?.invoke(values?.get(adapterPosition).toString())
                return@setOnLongClickListener true
            }
        }

        fun bindView(text : String){
            if(Workspace.activePhase.getChosenFilename().contains(text)){
                itemView.setBackgroundColor(Color.CYAN)
            }
            val messageButton =  itemView.findViewById<TextView>(R.id.audio_comment_title)
            messageButton.text = text

            val playButton = itemView.findViewById<ImageButton>(R.id.audio_comment_play_button)
            playButton.setOnClickListener {
                onPlayClick?.invoke(text, playButton)
            }

            val deleteButton = itemView.findViewById<ImageButton>(R.id.audio_comment_delete_button)
            deleteButton.setOnClickListener {
                onDeleteClick?.invoke(text)
            }
        }

    }
}