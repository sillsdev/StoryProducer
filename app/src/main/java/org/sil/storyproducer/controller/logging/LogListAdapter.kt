package org.sil.storyproducer.controller.logging

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import org.sil.storyproducer.R
import org.sil.storyproducer.model.logging.ComChkEntry
import org.sil.storyproducer.model.logging.DraftEntry
import org.sil.storyproducer.model.logging.LearnEntry
import org.sil.storyproducer.model.logging.Log
import org.sil.storyproducer.model.logging.LogEntry

import java.util.ArrayList
import java.util.TreeSet

internal class LogListAdapter(private val context: Context, log: Log?, slide: Int) : BaseAdapter() {

    private var displayEntries: Array<LogEntry>? = null
    private val learnEntries = ArrayList<LearnEntry>()
    private val draftEntries = ArrayList<DraftEntry>()
    private val comChkEntries = ArrayList<ComChkEntry>()

    init {
        if (log == null) {
            displayEntries = arrayOf()
        } else {
            if (slide < 0) {
                displayEntries = log.toTypedArray()
            } else {
                for (le in log) {
                    if (le.appliesToSlideNum(slide)) {
                        if (le is LearnEntry) {
                            learnEntries.add(le)
                        } else if (le is DraftEntry) {
                            draftEntries.add(le)
                        } else if (le is ComChkEntry) {
                            comChkEntries.add(le)
                        }
                    }
                }
                val sorter = TreeSet<LogEntry>()
                sorter.addAll(learnEntries)
                sorter.addAll(draftEntries)
                sorter.addAll(comChkEntries)
                displayEntries = sorter.toTypedArray()
            }
        }
    }

    fun updateList(learn: Boolean, draft: Boolean, comCheck: Boolean) {
        val sorter = TreeSet<LogEntry>()
        if (learn) {
            sorter.addAll(learnEntries)
        }
        if (draft) {
            sorter.addAll(draftEntries)
        }
        if (comCheck) {
            sorter.addAll(comChkEntries)
        }
        displayEntries = sorter.toTypedArray()
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return displayEntries!!.size
    }

    override fun getItem(position: Int): LogEntry {
        return displayEntries!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.log_list_item, parent, false)
        }

        val date = convertView!!.findViewById<TextView>(R.id.textView_logging_date)
        val info = convertView.findViewById<TextView>(R.id.textView_logging_type)

        val entry = getItem(position)
        date.text = entry.dateTime
        info.text = entry.phase + " - " + entry.description
        convertView.setBackgroundColor(ContextCompat.getColor(context, entry.color))

        return convertView
    }
}
