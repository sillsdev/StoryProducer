package org.sil.storyproducer.controller.storylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.sil.storyproducer.R


class FilterToolbarFrag: Fragment() {

    lateinit var filterChipGroup : ChipGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val toolbarView = inflater.inflate(R.layout.filter_toolbar, container, false)

        filterChipGroup = toolbarView.findViewById(R.id.filter_group)

        // Apply listeners to each of the Chips
        filterChipGroup.children.forEach { child ->
            (child as? Chip)?.setOnCheckedChangeListener { _, _ ->
                registerFilterChanged()
            }
        }
        return toolbarView
    }

    private fun registerFilterChanged() {
        // FILTER TODO: This needs to actually send data back to the StoryPageFragment
        val ids = filterChipGroup.checkedChipIds

        val titles = mutableListOf<CharSequence>()

        ids.forEach { id ->
            titles.add(filterChipGroup.findViewById<Chip>(id).text)
        }

        val text = if (titles.isNotEmpty()) {
            titles.joinToString(", ")
        } else {
            "No Choice"
        }

        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

}