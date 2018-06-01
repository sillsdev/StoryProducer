package org.sil.storyproducer.controller

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.CustomAdapter
import org.sil.storyproducer.model.ListFiles
import org.sil.storyproducer.model.SlideText
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.FileSystem
import org.sil.storyproducer.tools.file.ImageFiles
import org.sil.storyproducer.tools.file.TextFiles

class StoryListFrag : Fragment() {

    private var listView: ListView? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)

        val view: View

        // Define array storyNames to show in ListView
        val storyNames = FileSystem.getStoryNames()


        if (storyNames.size == 0) {
            view = inflater!!.inflate(R.layout.fragment_no_stories, container, false)
            return view
        }

        view = inflater!!.inflate(R.layout.activity_list_view, container, false)

        // Get ListView object from xml
        listView = activity.findViewById(R.id.story_list_view)


        val listFiles = arrayOfNulls<ListFiles>(storyNames.size)

        for (i in listFiles.indices) {
            val slideText = TextFiles.getSlideText(storyNames[i], 1)
            listFiles[i] = ListFiles(ImageFiles.getBitmap(storyNames[i], 1, 25), storyNames[i], slideText.subtitle)
        }

        val adapter = CustomAdapter(context, R.layout.story_list_item, listFiles)

        listView = view.findViewById(R.id.story_list_view)
        // Assign adapter to ListView
        listView!!.adapter = adapter

        listView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id -> (activity as MainActivity).switchToStory(storyNames[position]) }

        return view
    }

}
