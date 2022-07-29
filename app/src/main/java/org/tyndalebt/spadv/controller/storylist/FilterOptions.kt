package org.tyndalebt.spadv.controller.storylist

import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.model.Story
import org.tyndalebt.spadv.model.Workspace

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
    // DKH - 07/10/2021 - Issue 407: Add filtering to SP's 'Story Templates' List
    // Updated while integrating pull request #561 into current sillsdev baseline
    // Integration testing identified the stories in the displayed list were out of order
    //
    // These options show up on the "Story Template" list when the "ALL STORIES" tab is selected.
    // The Story Template list for all the tabs (ALL STORIES, IN PROGRESS, COMPLETED) are
    // grouped/sorted by the numeric string that precedes at title, eg: "002 Lost Coin" followed by
    // "006 Snakes Secret"
    // Currently, the three subcategories for the "ALL STORIES" tab are listed
    // below as enums (Other, OT, NT).  To keep with the paradigm of displaying the
    // "Story Template" list sorted by the numeric string at the beginning of the title,
    // (eg, 002 in title "002 Lost Coin") we have to match the enum values with the numeric
    // sort order that we want.  Here is the table
    //  ENUM            Subcategory Type            Range of values
    //    0                 Other                       0-99
    //    1                   OT                       100-199
    //    2                   NT                       200-299
    //
    //  If you set NT as enum 1 and OT as enum 2, when enabling or disabling the subcategories,
    //  the list will appear out of order, ie, the 200-299 range will appear before the 100-199.
    //
    // These names "Other", "OT" & "NT" are define in strings.xml

    Other(R.string.other_toolbar) {
        override fun getStoryList(): List<Story> {
            return Workspace.Stories.filter { story ->
                story.type == Story.StoryType.OTHER
            }
        }
    },

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
    };



    abstract fun getStoryList() : List<Story>
}