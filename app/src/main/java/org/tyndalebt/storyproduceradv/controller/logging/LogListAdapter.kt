package org.tyndalebt.storyproduceradv.controller.logging

import android.content.Context
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.model.PhaseType
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.model.logging.LogEntry

import java.util.ArrayList

internal class LogListAdapter(private val context: Context, slide: Int) : BaseAdapter() {

    private val allEntries = ArrayList<LogEntry>()
    private var displayEntries = ArrayList<LogEntry>()

    init {
        for (le in Workspace.activeStory.activityLogs) {
            if (le.appliesToSlideNum(slide)) {
                allEntries.add(le)
            }
        }
        displayEntries = allEntries
    }

    fun updateList(learn: Boolean, transRevise: Boolean, commWork: Boolean) {
        displayEntries = ArrayList()
        for (le in allEntries) {
            when(le.phase.phaseType){
                PhaseType.LEARN -> if(learn) displayEntries.add(le)
                PhaseType.TRANSLATE_REVISE -> if(transRevise) displayEntries.add(le)
                PhaseType.COMMUNITY_WORK -> if(commWork) displayEntries.add(le)
                else -> {}
            }
        }
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return displayEntries.size
    }

    override fun getItem(position: Int): LogEntry {
        return displayEntries[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var cView = convertView
        if (cView == null) {
            cView = LayoutInflater.from(context)
                    .inflate(R.layout.log_list_item, parent, false)
        }

        val date = cView!!.findViewById<TextView>(R.id.textView_logging_date)
        val info = cView.findViewById<TextView>(R.id.textView_logging_type)

        val entry = getItem(position)
        date.text = entry.dateTimeString
        info.text = "${entry.phase.getLangDisplayName(this.context)} - ${entry.description}"
        cView.setBackgroundColor(ContextCompat.getColor(context, entry.phase.getColor()))

        return cView
    }
}
