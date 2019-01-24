package org.sil.storyproducer.controller.keyterm

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.sil.storyproducer.R
import org.sil.storyproducer.model.BackTranslation
import org.sil.storyproducer.model.Workspace


class RecyclerDataAdapter(val context: Context?, private val recordings: MutableList<BackTranslation>) : RecyclerView.Adapter<RecyclerDataAdapter.MyViewHolder>() {

    var onItemClick: ((String) -> Unit)? = null
    var onItemLongClick: ((Int) -> Unit)? = null
    var onPlayClick: ((String, ImageButton) -> Unit)? = null
    var onDeleteClick: ((String, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.keyterm_audio_comment_list_item, parent, false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val audioText = recordings[position]
        holder.bindView(audioText)
    }

    override fun getItemCount(): Int = recordings.size

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var context: Context = itemView.context
        private var parentTextView: TextView = itemView.findViewById(R.id.audio_comment_title)
        private var parentPlayButton: ImageButton = itemView.findViewById(R.id.audio_comment_play_button)
        private var parentDeleteButton: ImageButton = itemView.findViewById(R.id.audio_comment_delete_button)
        private var frameLayoutChildItem: FrameLayout = itemView.findViewById(R.id.child_holder)
        val inflater: LayoutInflater = LayoutInflater.from(itemView.context)

        private val childComment = inflater.inflate(R.layout.backtranslation_comment_list_item, null, false)
        private val childSubmit = inflater.inflate(R.layout.submit_backtranslation_item, null, false)

        fun bindView(text : BackTranslation){
            parentTextView.text = text.audioBackTranslation.substringAfterLast("/")
            parentPlayButton.setOnClickListener {
                onPlayClick?.invoke(text.audioBackTranslation, parentPlayButton)
            }
            parentDeleteButton.setOnClickListener {
                onDeleteClick?.invoke(text.audioBackTranslation, adapterPosition)
            }
            parentTextView.setOnClickListener {
                onItemClick?.invoke(text.audioBackTranslation)
            }
            parentTextView.setOnLongClickListener {
                onItemLongClick?.invoke(adapterPosition)
                return@setOnLongClickListener true
            }

            if(recordings[adapterPosition].textBackTranslation != ""){
                initComment()
            }

            if(frameLayoutChildItem.tag == null){
                initSubmit()
            }
        }
        private fun initSubmit(){
            frameLayoutChildItem.removeAllViews()
            frameLayoutChildItem.addView(childSubmit)
            frameLayoutChildItem.tag = "submit"
            val addBacktranslation = itemView.findViewById<ImageButton>(R.id.submit_backtranslation_button)
            val editText = itemView.findViewById<EditText>(R.id.backtranslation_edit_text)
            addBacktranslation.setOnClickListener {
                if(editText.text.toString() != ""){
                    Workspace.activeKeyterm.backTranslations[adapterPosition].textBackTranslation = editText.text.toString()
                    editText.setText("")
                    frameLayoutChildItem.removeAllViews()
                    initComment()
                }
            }
        }
        private fun initComment(){
            frameLayoutChildItem.removeAllViews()
            frameLayoutChildItem.addView(childComment)
            frameLayoutChildItem.tag = "comment"
            val currentTextView = frameLayoutChildItem.findViewById(R.id.backtranslation_comment_title) as TextView
            currentTextView.text = recordings[adapterPosition].textBackTranslation
            val currentDeleteButton = frameLayoutChildItem.findViewById(R.id.backtranslation_comment_delete_button) as ImageButton
            currentDeleteButton.setOnClickListener {
                recordings[adapterPosition].textBackTranslation = ""
                frameLayoutChildItem.removeAllViews()
                initSubmit()
            }
        }
    }
}