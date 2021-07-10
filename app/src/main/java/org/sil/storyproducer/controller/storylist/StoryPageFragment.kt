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
import org.sil.storyproducer.service.SlideService

/**
 * StoryPageFragment is a flexible fragment in that it displays different things based on the
 * current configurations. Typically, this shows a list of list of stories. However, when there are
 * no stories present, a message is shown that notifies the user that there aren't any stories of
 * that type in the tab. Lastly, a StoryPageFragment can contain a FilterToolbarFrag that helps to
 * sort the list of stories in the adapter.
 *
 * StoryPageFragment has a one-one relationship with a StoryPageTab, however, since the
 * StoryPageFragment is an Android:Fragment, it will follow the typical activity lifecycle, which
 * means that it gets created and destroyed when it is not being used.
 */
class StoryPageFragment : Fragment() {

    private lateinit var storyPageTab : StoryPageTab
    private lateinit var listView: ListView
    private lateinit var adapter: ListAdapter
    // DKH - 07/10/2021 - Issue 407: Add filtering to SP's 'Story Templates' List
    // Updated while integrating pull request #561 into current sillsdev baseline
    // Integration testing identified the wrong index was being used for the selected story
    //
    // The story list, as supplied by StoryPageTab, can be modified by FilterToolbarFrag
    // via updateStoryList.  So, we stash a copy of the story list in CurrentStoryList so that
    // when a click is performed on a story, we know which story was actually clicked on.
    // CurrentStoryList is used below to start a new activity on a selected story
    private var CurrentStoryList: List<Story> = emptyList()

    companion object {
        const val ARG_POSITION = "position"

        /**
         * Creates a new instance based off of the tab position parameter
         * @param position The Tab Position
         */
        fun getInstance(position: Int): StoryPageFragment {
            val storyPageFragment = StoryPageFragment()
            val bundle = Bundle()
            bundle.putInt(ARG_POSITION, position)
            storyPageFragment.arguments = bundle
            return storyPageFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)

        val position = requireArguments().getInt(ARG_POSITION)
        storyPageTab = StoryPageTab.values()[position]

        // DKH - 07/10/2021 - Issue 407: Add filtering to SP's 'Story Templates' List
        // Updated while integrating pull request #561 into current sillsdev baseline
        //
        // save the stories assoicated with this storyPageFragment
        // (eg, ALL STORIES, IN PROGRESS, COMPLETED)
        CurrentStoryList = storyPageTab.getStoryList()  // grab the stories
        if (CurrentStoryList.isEmpty()) {  // If empty, set up for displaying "No Story" message
            val view = inflater.inflate(R.layout.fragment_no_stories, container, false)

            view!!.findViewById<TextView>(R.id.stories_not_found_text).text =
                    if (Build.VERSION.SDK_INT >= 24){
                        Html.fromHtml(getString(storyPageTab.emptyStoryStringId), 0)}
                    else{
                        Html.fromHtml(getString(storyPageTab.emptyStoryStringId))}

            val button : Button = view.findViewById(R.id.update_workspace_button)
            if(storyPageTab != StoryPageTab.ALL_STORIES) {
                button.visibility = View.INVISIBLE
            } else {
                button.setOnClickListener {
                    (activity as? BaseActivity)?.showSelectTemplatesFolderDialog()
                }
            }

            return view
        }

        val lfview = inflater.inflate(R.layout.story_list_container, container, false)

        // Apply the Stories to the Story List View
        adapter = ListAdapter(context!!, R.layout.story_list_item, storyPageTab.getStoryList(), storyPageTab)

        listView = lfview.findViewById(R.id.story_list_view)

        // DKH - 07/10/2021 - Issue 407: Add filtering to SP's 'Story Templates' List
        // Updated while integrating pull request #561 into current sillsdev baseline
        //

        // Set up a lambda routine to execute when a user clicks on a story.
        // Upon a story click, the story is set as the current story and a new
        // activity is started using the selected story.  Use CurrentStoryList to
        // determine which story
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                (activity as MainActivity).switchToStory(CurrentStoryList[position]) }

        // Assign adapter to ListView
        listView.adapter = adapter

        return lfview
    }

    /**
     * Updates ListAdapter to use the newly provided list. This is very helpful when filter options
     * are used.
     * @param storyList List of new stories to be used in the ListAdapter
     */
    fun updateStoryList(storyList: List<Story>) {
        // DKH - 07/10/2021 - Issue 407: Add filtering to SP's 'Story Templates' List
        // Updated while integrating pull request #561 into current sillsdev baseline
        //
        // Update CurrentStoryList so that when a click is made on a story, we know which
        // story is selected
        CurrentStoryList = storyList
        adapter = ListAdapter(context!!, R.layout.story_list_item, storyList, storyPageTab)
        listView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if(storyPageTab.hasFilterToolbar && storyPageTab.getStoryList().isNotEmpty()) {
            val childFragment: Fragment = FilterToolbarFrag(this)
            val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
            transaction.replace(R.id.filter_container, childFragment).commit()
        }
    }

}

class ListAdapter(context: Context,
                  private val resourceId: Int,
                  private val stories: List<Story>,
                  private val storyPageTab: StoryPageTab) : ArrayAdapter<Story>(context, resourceId, stories) {

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

            // Handle graying out text when story is completed
            if(storyPageTab == StoryPageTab.ALL_STORIES && story.isComplete) {
                holder.txtTitle.alpha = 0.5f
                holder.txtSubTitle.alpha = 0.5f
            } else {
                holder.txtTitle.alpha = 1f
                holder.txtSubTitle.alpha = 1f
            }

            // Handle the image icon to the side of the story
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && storyPageTab == StoryPageTab.ALL_STORIES) { // Only show icon on ALL Stories

                val color = if(story.isComplete) {
                    R.color.story_list_completed
                } else if(story.inProgress && !story.isComplete) {
                    R.color.story_list_in_progress
                } else {
                    null
                }

                val progressIcon : ImageView = row.findViewById(R.id.progress_icon)

                if(color == null) {
                    progressIcon.visibility = View.INVISIBLE
                } else {
                    progressIcon.visibility = View.VISIBLE
                    progressIcon.setBackgroundColor(context.getColor(color))
                }
            }
        }

        return row
    }

    internal class FileHolder(view: View){
        var imgIcon: ImageView = view.findViewById(R.id.story_list_image)
        var txtTitle: TextView = view.findViewById(R.id.story_list_title)
        var txtSubTitle: TextView = view.findViewById(R.id.story_list_subtitle)
    }

}