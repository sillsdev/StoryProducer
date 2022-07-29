package org.tyndalebt.spadv.controller.storylist

import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.model.Story
import org.tyndalebt.spadv.model.Workspace

/**
 * Contains all of the tabs that are added to the main page. This has been created such that if you
 * want to add another tab, all you need to do is edit this file and it will dynamically appear on
 * the story list page. There is a one-one correlation between the enum values below and a
 * StoryPageFragment.
 */
enum class StoryPageTab(val nameId: Int,
                        val emptyStoryStringId: Int,
                        val hasFilterToolbar : Boolean) {

    ALL_STORIES(R.string.all_stories_tab, R.string.stories_not_found_body, true) {
        override fun getStoryList(): List<Story> {
            return Workspace.Stories
        }
    },

    IN_PROGRESS(R.string.in_progress_tab, R.string.stories_not_found_in_progress, false) {
        override fun getStoryList(): List<Story> {
            return Workspace.Stories.filter {
                story -> story.inProgress && !story.isComplete
            }
        }
    },

    COMPLETED(R.string.completed_tab, R.string.stories_not_found_completed, false) {
        override fun getStoryList(): List<Story> {
            return Workspace.Stories.filter {
                story -> story.isComplete
            }
        }
    };

    // Allow the story list to be dynamically generated each time
    // this allows the story to be updated
    abstract fun getStoryList() : List<Story>
}