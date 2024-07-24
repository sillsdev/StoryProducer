package org.sil.storyproducer.controller.bldownload

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.bldownload.BLCustomAdapter.BLViewHolder
import org.sil.storyproducer.controller.bldownload.BLDownloadActivity.Companion.primaryLang
import org.sil.storyproducer.model.BLBookList
import org.sil.storyproducer.model.thumbnailsAutoDLDir
import timber.log.Timber
import java.io.File

// This class implements a RecyclerView.Adapter for the Bloom Library card list UI
// The child class BLViewHolder is used to hold the UI widgets that make the card item
class BLCustomAdapter(bldata: ArrayList<BLDataModel>, langFilter : String) : RecyclerView.Adapter<BLViewHolder?>() {
    private val dataSet: ArrayList<BLDataModel>

    init {
        dataSet = bldata.filter { langFilter.isEmpty() || primaryLang(it.lang) == langFilter } as ArrayList<BLDataModel>
    }

    class BLViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewTitle: TextView
        var textViewLang: TextView
        var imageViewThumbnailIcon: ImageView
        var imageViewCheckBox: ImageView
        var imageViewCardParent: BLCheckableCardView

        init {
            textViewTitle = itemView.findViewById<View>(R.id.textViewTitle) as TextView
            textViewLang = itemView.findViewById<View>(R.id.textViewLang) as TextView
            imageViewThumbnailIcon = itemView.findViewById<View>(R.id.imageViewThumb) as ImageView
            imageViewCheckBox = itemView.findViewById<View>(R.id.imageViewCheckBox) as ImageView
            imageViewCardParent = itemView.findViewById<View>(R.id.card_view) as BLCheckableCardView
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BLViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.bldl_cards_layout, parent, false)
        view.setOnClickListener(BLDownloadActivity.bldlActivity.blOnClickListener)
        return BLViewHolder(view)
    }

    override fun onBindViewHolder(holder: BLViewHolder, listPosition: Int) {
        holder.textViewTitle.text = dataSet[listPosition].title
        holder.textViewLang.text = dataSet[listPosition].lang

        // show check box is visible or not by setting alpha (transparency) property
        holder.imageViewCheckBox.alpha = if (dataSet[listPosition].isChecked)  1.0F else 0.0F

        // show grayed-out (disabled) or not by setting alpha (transparency) property
        // 0.5 is grayed-out (since background is black)
        holder.imageViewCardParent.alpha = if (dataSet[listPosition].isEnabled) 1.0F else 0.5F

        // now we have to display the thumbnail image if it has been downloaded
        var downloadedImage: Bitmap? = null
        if (dataSet[listPosition].thumbnailDownloaded) {
            val id = BLBookList.extractThumbnailId(dataSet[listPosition].thumbnailUri)
            if (!id.isNullOrEmpty()) {
                try {
                    val downloadedThumbnail = thumbnailsAutoDLDir() + "${id}.png"
                    if (File(downloadedThumbnail).exists())
                        downloadedImage = BitmapFactory.decodeFile(downloadedThumbnail)
                } catch (e: Exception) {
                    Timber.w("Exception loading downloaded thumbnail: $id, error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        if (downloadedImage != null)
            holder.imageViewThumbnailIcon.setImageBitmap(downloadedImage)
        else
            holder.imageViewThumbnailIcon.setImageResource(dataSet[listPosition].imageId)

    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

}
