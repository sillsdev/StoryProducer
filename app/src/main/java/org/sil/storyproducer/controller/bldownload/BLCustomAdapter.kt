package org.sil.storyproducer.controller.bldownload

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.bldownload.BLCustomAdapter.BLViewHolder
import java.util.ArrayList

// This class implements a RecyclerView.Adapter for the Bloom Library card list UI
// The child class BLViewHolder is used to hold the UI widgets that make the card item
class BLCustomAdapter(data: ArrayList<BLDataModel>) : RecyclerView.Adapter<BLViewHolder?>() {
    private val dataSet: ArrayList<BLDataModel>

    init {
        dataSet = data
    }

    class BLViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewTitle: TextView
        var textViewLang: TextView
        var imageViewThumbnailIcon: ImageView
        var imageViewCheckBox: ImageView

        init {
            textViewTitle = itemView.findViewById<View>(R.id.textViewTitle) as TextView
            textViewLang = itemView.findViewById<View>(R.id.textViewLang) as TextView
            imageViewThumbnailIcon = itemView.findViewById<View>(R.id.imageViewThumb) as ImageView
            imageViewCheckBox = itemView.findViewById<View>(R.id.imageViewCheckBox) as ImageView
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BLViewHolder {
        val view: View = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.bldl_cards_layout, parent, false)
        view.setOnClickListener(BLDownloadActivity.bldlActivity.blOnClickListener)
        return BLViewHolder(view)
    }

    override fun onBindViewHolder(holder: BLViewHolder, listPosition: Int) {
        holder.textViewTitle.text = dataSet[listPosition].title
        holder.textViewLang.text = dataSet[listPosition].lang
        holder.imageViewThumbnailIcon.setImageResource(dataSet[listPosition].imageId)
        holder.imageViewCheckBox.alpha = if (dataSet[listPosition].isChecked)  1.0F else 0.0F // show check box or not
        holder.itemView.alpha = if (dataSet[listPosition].isInWorkspace)  0.5F else 1.0F // 'grayed' out if disabled (since background is black)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

}
