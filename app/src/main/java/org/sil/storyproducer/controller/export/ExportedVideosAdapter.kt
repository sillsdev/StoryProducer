package org.sil.storyproducer.controller.export

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView

import org.sil.storyproducer.R
import org.sil.storyproducer.model.VIDEO_DIR
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.tools.file.getWorkspaceUri

import java.util.ArrayList

class ExportedVideosAdapter(private val context: Context) : BaseAdapter() {

    private var videoPaths: List<String> = ArrayList()
    private val mInflater: LayoutInflater

    init {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    fun setVideoPaths(paths: List<String>) {
        videoPaths = paths
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return videoPaths.size
    }

    override fun getItem(position: Int): String {
        return videoPaths[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val path = videoPaths[position]

        //split the path so we can get just the file name witch will be used in the view
        val splitPath = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val fileName = splitPath[splitPath.size - 1]

        //recreate the holder every time because the views are changing around

        val rowView = mInflater.inflate(R.layout.exported_video_row, null)
        val holder = RowViewHolder()
        holder.textView = rowView.findViewById(R.id.video_title)
        holder.playButton = rowView.findViewById(R.id.video_play_button)!!
        holder.shareButton = rowView.findViewById(R.id.file_share_button)

        //set the two different button listeners
        holder.playButton!!.setOnClickListener { showPlayVideoChooser(path) }
        holder.shareButton!!.setOnClickListener { showShareFileChooser(path, fileName) }
        rowView.tag = holder

        holder.textView!!.text = fileName

        return rowView
    }

    class RowViewHolder {
        var textView: TextView? = null
        var playButton: ImageButton? = null
        var shareButton: ImageButton? = null
    }

    private fun showPlayVideoChooser(path: String) {
        val videoIntent = Intent(android.content.Intent.ACTION_VIEW)
        val uri = getWorkspaceUri("$VIDEO_DIR/$path")
        //TODO fix this so it actually plays.  Why not?
        videoIntent.setDataAndNormalize(uri)
        videoIntent.putExtra(Intent.EXTRA_STREAM, uri)
        videoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(videoIntent, context.getString(R.string.file_view)))
    }

    private fun showShareFileChooser(path: String, fileName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "video/*"
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
        shareIntent.putExtra(android.content.Intent.EXTRA_TITLE, fileName)
        val uri = getWorkspaceUri("$VIDEO_DIR/$path")
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        //TODO replace with documentLaunchMode for the activity to make compliant with API 18
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.send_video)))
    }

}
