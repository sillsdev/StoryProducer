package org.sil.storyproducer.controller.storylist

import org.sil.storyproducer.R
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace

enum class StoryPageTab(val tabNameId: Int,
                        val hasFilterToolbar : Boolean) {

    ALL_STORIES(R.string.all_stories_tab, true) {
        override fun getStoryList(): List<Story> {
            return Workspace.Stories
        }
    },

    IN_PROGRESS(R.string.in_progress_tab, false) {
        override fun getStoryList(): List<Story> {
            return Workspace.Stories.filter { story -> !story.isApproved }
        }
    },

    COMPLETED(R.string.completed_tab, false) {
        override fun getStoryList(): List<Story> {
            return Workspace.Stories.filter { story -> story.isApproved }
        }
    };

    // Allow the story list to be dynamically generated each time
    // this allows the story to be updated
    abstract fun getStoryList() : List<Story>
}