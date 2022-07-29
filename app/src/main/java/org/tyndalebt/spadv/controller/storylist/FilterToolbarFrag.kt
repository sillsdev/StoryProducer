package org.tyndalebt.spadv.controller.storylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.model.Story

/**
 * FilterToolbarFrag is the child filter element that is contained in a StoryListFragment.
 * The FilterToolbarFrag dynamically generates a list of Material.io Choice Chips from FilterOptions.
 * These Chips are clickable buttons that notify the parent StoryListFragment when they are clicked
 * causing a new update of the story list in the StoryListFragment ListAdapter.
 */
class FilterToolbarFrag(private val storyPageFrag : StoryPageFragment): Fragment() {

    private lateinit var filterChipGroup : ChipGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val toolbarView = inflater.inflate(R.layout.filter_toolbar, container, false)

        filterChipGroup = toolbarView.findViewById(R.id.filter_group)

        // Create all the Chips dynamically
        FilterOptions.values().forEach { option ->
            val chip = inflater.inflate(R.layout.filter_chip_choice, filterChipGroup, false) as Chip
            chip.text = getString(option.nameId)
            chip.id = option.ordinal
            filterChipGroup.addView(chip)

            // Apply listeners to each of the Chips
            chip.setOnCheckedChangeListener { _, _ ->
                registerFilterChanged()
            }
        }

        return toolbarView
    }

    private fun registerFilterChanged() {
        var newStoryList = mutableListOf<Story>()

        filterChipGroup.checkedChipIds.forEach { id ->
            val filterStoryList = FilterOptions.values()[id].getStoryList()
            newStoryList.addAll(filterStoryList)
        }

        // Remove all duplicate values
        newStoryList = newStoryList.distinct().toMutableList()

        // Update parent fragment with generated story list
        storyPageFrag.updateStoryList(newStoryList)
    }

}