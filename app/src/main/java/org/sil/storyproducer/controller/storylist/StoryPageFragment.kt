package org.sil.storyproducer.controller.storylist

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import org.sil.storyproducer.R
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.service.SlideService
import java.util.*
import kotlin.collections.HashMap

class StoryPageFragment : Fragment() {

    private lateinit var storyPageTab : StoryPageTab

    companion object {
        const val ARG_POSITION = "position"

        fun getInstance(position: Int): Fragment {
            val storyPageFragment = StoryPageFragment()
            val bundle = Bundle()
            bundle.putInt(ARG_POSITION, position)
            storyPageFragment.arguments = bundle
            return storyPageFragment
        }
    }

    lateinit var adapter: ListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)

        val position = requireArguments().getInt(ARG_POSITION)
        storyPageTab = StoryPageTab.values()[position]

        if (storyPageTab.getStoryList().isEmpty()) {
            val view = inflater.inflate(R.layout.fragment_no_stories, container, false)

            view!!.findViewById<TextView>(R.id.stories_not_found_text).text =
                    if (Build.VERSION.SDK_INT >= 24){
                        Html.fromHtml(getString(R.string.stories_not_found_body), 0)}
                    else{
                        Html.fromHtml(getString(R.string.stories_not_found_body))}

            view.findViewById<Button>(R.id.update_workspace_button).setOnClickListener {
                (activity as? BaseActivity)?.showSelectTemplatesFolderDialog()
            }
            return view
        }

        val lfview = inflater.inflate(R.layout.story_list_container, container, false)

        // Apply the Stories to the Story List View
        adapter = ListAdapter(context!!, R.layout.story_list_item, storyPageTab.getStoryList() as MutableList<Story>)

        val listView = lfview.findViewById<ListView>(R.id.story_list_view)

        listView.onItemClickListener = AdapterView.OnItemClickListener {_, _, position, _ ->
            (activity as MainActivity).switchToStory(storyPageTab.getStoryList()[position])
        }

        // Assign adapter to ListView
        listView.adapter = adapter

        return lfview
    }

    fun notifyDataSetChanged() {
        adapter.notifyDataSetChanged()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if(storyPageTab.hasFilterToolbar) {
            val childFragment: Fragment = FilterToolbarFrag()
            val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
            transaction.replace(R.id.filter_container, childFragment).commit()
        }
    }

}

class ListAdapter(context: Context, private val resourceId: Int, private val stories: MutableList<Story>) : ArrayAdapter<Story>(context, resourceId, stories) {

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
            holder.txtTitle.text = story.title
            //TODO put th number 25 in some configuration.  What if the images are different sizes?
            //Use the "second" image, because the first is just for the title screen.
            holder.imgIcon.setImageBitmap(SlideService(context).getImage(1, 25, story))
            holder.txtSubTitle.text = story.slides[0].subtitle
        }

        return row
    }

    internal class FileHolder(view: View){
        var imgIcon: ImageView = view.findViewById(R.id.story_list_image)
        var txtTitle: TextView = view.findViewById(R.id.story_list_title)
        var txtSubTitle: TextView = view.findViewById(R.id.story_list_subtitle)
    }

}