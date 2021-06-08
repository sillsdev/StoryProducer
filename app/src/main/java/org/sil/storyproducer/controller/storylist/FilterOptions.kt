package org.sil.storyproducer.controller.storylist

import org.sil.storyproducer.R
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace

/**
 * Contains all of the Filter Options for the main toolbar. To add a new filter option to the story
 * menu, create a new string with the title and add it to /res/value/strings.xml. Next, add the
 * types of stories that should be applied when the option is clicked
 *
 * Example:
 *
 * Film(R.string.film_toolbar) {
 *  override fun getStoryList(): List<Story> {
 *
 *  // Tells what stories should be added when clicked
 *      return Workspace.Stories.filter { story ->
 *          story.isFilmStory
 *      }
 *  }
 */
enum class FilterOptions(val nameId: Int) {
    OT(R.string.ot_toolbar) {
        override fun getStoryList(): List<Story> {
            return Workspace.Stories.filter { story ->
                story.type == Story.StoryType.OLD_TESTAMENT
            }
        }
    },

    NT(R.string.nt_toolbar) {
        override fun getStoryList(): List<Story> {
            return Workspace.Stories.filter { story ->
                story.type == Story.StoryType.NEW_TESTAMENT
            }
        }
    },

    Other(R.string.other_toolbar) {
        override fun getStoryList(): List<Story> {
            return Workspace.Stories.filter { story ->
                story.type == Story.StoryType.OTHER
            }
        }
    };

    abstract fun getStoryList() : List<Story>
}