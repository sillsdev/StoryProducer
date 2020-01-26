package org.sil.storyproducer.controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.genDefaultImage
import org.sil.storyproducer.tools.file.getStoryImage

class StoryListFrag : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        if (Workspace.Stories.isEmpty()) {
            val rootView = inflater.inflate(R.layout.fragment_no_stories, container, false)
            rootView.findViewById<TextView>(R.id.stories_not_found_text).text = Html.fromHtml(getString(R.string.stories_not_found_body))
            rootView.findViewById<Button>(R.id.update_workspace_button).setOnClickListener {
                val intent = Intent(activity, WorkspaceUpdateActivity::class.java)
                activity!!.startActivity(intent)
                activity!!.finish()
            }
            return rootView
        } else {
            val rootView = inflater.inflate(R.layout.activity_list_view, container, false)
            val listView = rootView.findViewById<ListView>(R.id.story_list_view)
            val adapter = ListAdapter(context!!, R.layout.story_list_item, Workspace.Stories)
            listView.adapter = adapter
            listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                Workspace.activeStory = Workspace.Stories[position]
                val intent = Intent(activity!!.applicationContext, PhaseBaseActivity::class.java)
                startActivity(intent)
            }
            return rootView
        }
    }
}

class ListAdapter(context: Context, private val resourceId: Int, private val stories: MutableList<Story>) : ArrayAdapter<Story>(context, resourceId, stories) {

    override fun getView(position: Int, itemView: View?, parent: ViewGroup): View {
        var row = itemView
        val holder: FileHolder

        if (row == null) {
            val inflater = (context as Activity).layoutInflater
            row = inflater.inflate(resourceId, parent, false)

            holder = FileHolder(row!!)
            row.tag = holder
        } else {
            holder = row.tag as FileHolder
        }

        if (position <= stories.size) {
            val story = stories[position]
            holder.txtTitle.text = story.title
            //TODO put th number 25 in some configuration.  What if the images are different sizes?
            //Use the "second" image, because the first is just for the title screen.
            var bitmap: Bitmap? = null
            var i = 1
            while (i < story.slides.size && bitmap == null) {
                bitmap = getStoryImage(context, i, 25, story)
                i++
            }
            holder.imgIcon.setImageBitmap(bitmap ?: genDefaultImage())
            holder.txtSubTitle.text = story.slides[0].subtitle
        }

        return row
    }

    internal class FileHolder(view: View) {
        var imgIcon: ImageView = view.findViewById(R.id.story_list_image)
        var txtTitle: TextView = view.findViewById(R.id.story_list_title)
        var txtSubTitle: TextView = view.findViewById(R.id.story_list_subtitle)
    }

}
