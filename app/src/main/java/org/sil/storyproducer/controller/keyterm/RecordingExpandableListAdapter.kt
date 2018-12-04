package org.sil.storyproducer.controller.keyterm

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.model.BackTranslation

class RecordingExpandableListAdapter(val context: Context?, private val recordings: MutableList<BackTranslation>) : BaseExpandableListAdapter(){

    override fun getChild(groupPosition: Int, childPosititon: Int): Any {
        return recordings[groupPosition].textBackTranslation[childPosititon]
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getChildView(groupPosition: Int, childPosition: Int,
                              isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        var childView = convertView
        if (childView == null) {
            val infalInflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            childView = infalInflater.inflate(R.layout.backtranslation_comment_list_item, null)
        }

        val backtranslationCommentTitle = childView?.findViewById<TextView>(R.id.backtranslation_comment_title)
        backtranslationCommentTitle?.text = getChild(groupPosition, childPosition) as String

        val audioCommentDeleteButton = childView?.findViewById<ImageButton>(R.id.backtranslation_comment_delete_button)
        audioCommentDeleteButton?.setOnClickListener {
            recordings[groupPosition].textBackTranslation.removeAt(childPosition)
        }

        return childView!!
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return recordings[groupPosition].textBackTranslation.size
    }

    override fun getGroup(groupPosition: Int): Any {
        return recordings[groupPosition]
    }

    override fun getGroupCount(): Int {
        return recordings.size
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean,
                              convertView: View?, parent: ViewGroup): View {
        var groupView = convertView
        if (groupView == null) {
            val infalInflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            groupView = infalInflater.inflate(R.layout.keyterm_audio_comment_list_item, null)
        }

        val audioCommentTitle = groupView?.findViewById<TextView>(R.id.audio_comment_title)
        val group = getGroup(groupPosition) as BackTranslation
        audioCommentTitle?.text = group.audioBackTranslation

        return groupView!!
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }
}

class RecyclerDataAdapter(val context: Context?, private val recordings: MutableList<BackTranslation>) : RecyclerView.Adapter<RecyclerDataAdapter.MyViewHolder>() {

    var onItemClick: ((String) -> Unit)? = null
    var onItemLongClick: ((Int) -> Unit)? = null
    var onPlayClick: ((String, ImageButton) -> Unit)? = null
    var onDeleteClick: ((String, Int) -> Unit)? = null
    var currentPosition: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.keyterm_audio_comment_list_item, parent, false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val audioText = recordings[position]
        holder.bindView(audioText)


        //set toggle
        if (currentPosition == position) {
            holder.linearLayoutChildItems.visibility = View.VISIBLE
        }
        else{
            holder.linearLayoutChildItems.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = recordings.size

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var context: Context = itemView.context
        var parentTextView: TextView = itemView.findViewById(R.id.audio_comment_title)
        var parentPlayButton: ImageButton = itemView.findViewById(R.id.audio_comment_play_button)
        var parentDeleteButton: ImageButton = itemView.findViewById(R.id.audio_comment_delete_button)
        var linearLayoutChildItems: LinearLayout = itemView.findViewById(R.id.child_holder)
        val inflater: LayoutInflater = LayoutInflater.from(itemView.context)

        init {
            linearLayoutChildItems.visibility = View.GONE
            var intMaxNoOfChild = 0
            for (i in 0 until recordings.size){
                val intMaxSizeTemp = recordings[i].textBackTranslation.size
                if(intMaxSizeTemp > intMaxNoOfChild) intMaxNoOfChild = intMaxSizeTemp
            }
            for (i in 0..intMaxNoOfChild){
                val childItem =  inflater.inflate(R.layout.backtranslation_comment_list_item, null, false)
                linearLayoutChildItems.addView(childItem)
            }

            parentTextView.setOnClickListener {
                if (linearLayoutChildItems.visibility == View.INVISIBLE){
                    linearLayoutChildItems.visibility = View.GONE
                }
                else{
                    linearLayoutChildItems.visibility = View.VISIBLE
                }
            }
        }

        fun bindView(text : BackTranslation){
            parentTextView.text = text.audioBackTranslation
            parentPlayButton.setOnClickListener {
                onPlayClick?.invoke(text.audioBackTranslation, parentPlayButton)
            }
            parentDeleteButton.setOnClickListener {
                onDeleteClick?.invoke(text.audioBackTranslation, adapterPosition)
            }
            parentTextView.setOnClickListener {
                onItemClick?.invoke(text.audioBackTranslation)
                currentPosition = adapterPosition
                notifyDataSetChanged()
            }
            parentTextView.setOnLongClickListener {
                onItemLongClick?.invoke(adapterPosition)
                return@setOnLongClickListener true
            }

            val noOfChildTextViews = linearLayoutChildItems.childCount
            val noOfChild = recordings[adapterPosition].textBackTranslation.size
            if(noOfChild < noOfChildTextViews){
                for(i in noOfChild until noOfChildTextViews){
                    val currentTextView = linearLayoutChildItems.getChildAt(i)
                    currentTextView.visibility = View.GONE
                }
            }
            for(i in 0 until noOfChild){
                val currentTextView = linearLayoutChildItems.getChildAt(i).findViewById(R.id.backtranslation_comment_title) as TextView
                currentTextView.text = text.textBackTranslation[i]
                val currentDeleteButton = linearLayoutChildItems.getChildAt(i).findViewById(R.id.backtranslation_comment_delete_button) as ImageButton
                currentDeleteButton.setOnClickListener {
                    recordings[adapterPosition].textBackTranslation.removeAt(i)
                    notifyDataSetChanged()
                }
            }
        }
    }
}