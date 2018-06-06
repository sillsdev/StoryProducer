package org.sil.storyproducer.controller

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import org.sil.storyproducer.R
import org.sil.storyproducer.model.*

class StoryListFrag : Fragment() {

    private var listView: ListView? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)

        if (Workspace.Stories.isEmpty()) return inflater!!.inflate(R.layout.fragment_no_stories, container, false)

        val lfview = inflater!!.inflate(R.layout.activity_list_view, container, false)

        // Get ListView object from xml
        listView = activity.findViewById(R.id.story_list_view)

        val adapter = ListAdapter(context, R.layout.story_list_item, Workspace.Stories)

        listView = lfview.findViewById(R.id.story_list_view)
        // Assign adapter to ListView
        listView!!.adapter = adapter

        //TODO remove "switchtostory" call.  That is still from the old template way.
        listView!!.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ -> (activity as MainActivity).switchToStory(Workspace.Stories[position].title) }

        return lfview
    }

}

class ListAdapter(context: Context,
                  val resourceId: Int,
                  val stories: MutableList<Story>) : ArrayAdapter<Story>(context, resourceId, stories) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = convertView
        val holder: FileHolder

        if (row == null) {
            val inflater = (context as Activity).layoutInflater
            row = inflater.inflate(resourceId, parent, false)

            holder = FileHolder(row!!)
            row.tag = holder
        } else {
            holder = row.tag as FileHolder
        }

        if(position <= stories.size){
            val story = stories[position]
            holder.txtTitle.text = story.slides[0].title
            //TODO put th number 25 in some configuration.  What if the images are different sizes?
            //Use the "second" image, because the first is just for the title screen.
            holder.imgIcon.setImageBitmap(story.getImage(1,25))
            holder.txtSubTitle.text = story.slides[0].subtitle
        }

        return row
    }

    internal class FileHolder{
        var imgIcon: ImageView
        var txtTitle: TextView
        var txtSubTitle: TextView
        constructor(view : View){
            imgIcon = view.findViewById(R.id.story_list_image)
            txtTitle = view.findViewById(R.id.story_list_title)
            txtSubTitle = view.findViewById(R.id.story_list_subtitle)
        }
    }

}
