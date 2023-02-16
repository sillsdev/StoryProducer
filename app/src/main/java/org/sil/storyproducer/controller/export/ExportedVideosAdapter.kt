package org.sil.storyproducer.controller.export

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.FileProvider
import org.sil.storyproducer.App
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.R
import org.sil.storyproducer.model.VIDEO_DIR
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.getWorkspaceUri
import java.io.File
import java.util.*


class ExportedVideosAdapter(private val context: Context, private val rvListener: RefreshViewListener) : BaseAdapter() {

    private var videoPaths: List<String> = ArrayList()
    private val mInflater: LayoutInflater

    init {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    fun setVideoPaths(paths: List<String>) {
        val tempVideos : MutableList<String> = ArrayList()
        for (i in 0 until paths.size){
            if(paths[i] in Workspace.activeStory.outputVideos){
                tempVideos.add(paths[i])
            }
        }
        videoPaths = tempVideos
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
        holder.deleteButton = rowView.findViewById(R.id.file_delete_button)

        //set the two different button listeners
        holder.playButton!!.setOnClickListener { showPlayVideoChooser(path) }
        holder.shareButton!!.setOnClickListener { showShareFileChooser(path, fileName) }
        holder.deleteButton!!.setOnClickListener { showDeleteDialog(path) }
        rowView.tag = holder

        holder.textView!!.text = fileName

        return rowView
    }

    class RowViewHolder {
        var textView: TextView? = null
        var playButton: ImageButton? = null
        var shareButton: ImageButton? = null
        var deleteButton: ImageButton? = null
    }

    private fun showPlayVideoChooser(path: String) {
        val videoIntent = Intent(android.content.Intent.ACTION_VIEW)
        var uri = getWorkspaceUri("$VIDEO_DIR/$path")
        // Should now be fixed so it actually plays.
        // Why did it not work? - because it needs the new grantUriPermission() etc.
        if (uri?.scheme == "file") {
            // for internal storage we need a content: Uri
            uri = FileProvider.getUriForFile(App.Companion.appContext,
                    BuildConfig.APPLICATION_ID + ".fileprovider", File(uri.path))
        }

        videoIntent.setDataAndType(uri, "video/*")
        videoIntent.putExtra(Intent.EXTRA_STREAM, uri)
        videoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val chooser = Intent.createChooser(videoIntent, context.getString(R.string.file_view))
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // find all packages that can respond to this view intent
        val resInfoList: List<ResolveInfo> = context.getPackageManager()
                .queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)

        for (resolveInfo in resInfoList) {
            // grant each package read uri permission for this file/uri
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }

    private fun showShareFileChooser(path: String, fileName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "video/*"
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
        shareIntent.putExtra(android.content.Intent.EXTRA_TITLE, fileName)
        var uri = getWorkspaceUri("$VIDEO_DIR/$path")
        if (uri?.scheme == "file") {
            // for internal storage we need a content: Uri
            uri = FileProvider.getUriForFile(App.Companion.appContext,
                BuildConfig.APPLICATION_ID + ".fileprovider", File(uri.path))
        }
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        //TODO replace with documentLaunchMode for the activity to make compliant with API 18
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)

        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.send_video))
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // find all packages that can respond to this send intent
        val resInfoList: List<ResolveInfo> = context.getPackageManager()
            .queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)

        for (resolveInfo in resInfoList) {
            // grant each package read uri permission for this file/uri
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }

    private fun showDeleteDialog(path: String) {
        val dialog = AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.delete_video_title))
                .setMessage(context.getString(R.string.delete_video_message))
                .setNegativeButton(context.getString(R.string.no), null)
                .setPositiveButton(context.getString(R.string.yes)) { _, _ ->
                    Workspace.deleteVideo(context,path)
                    rvListener.refreshViews()
                }
                .create()

        dialog.show()
    }

}

interface RefreshViewListener {
    fun refreshViews()
}

