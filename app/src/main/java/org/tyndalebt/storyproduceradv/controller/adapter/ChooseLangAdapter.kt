package org.tyndalebt.storyproduceradv.controller.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.BaseActivity
import org.tyndalebt.storyproduceradv.activities.ChooseLangActivity
import org.tyndalebt.storyproduceradv.model.Workspace

class ChooseLangAdapter(data: ArrayList<DownloadDS?>, var cla: ChooseLangActivity) :
    ArrayAdapter<DownloadDS?>(cla, R.layout.bloom_list_item, data), View.OnClickListener {
    var mContext: Context
    private val dataSetArray: ArrayList<DownloadDS?>

    private class ViewHolder {
        var chkItem: CheckedTextView? = null
    }

    override fun onClick(v: View) {
        val position = v.tag as Int
        val `object`: Any? = getItem(position)
        val dataModel = `object` as DownloadDS?

        cla.setLanguage(dataModel!!.URL) // language name in English
        if (Workspace.isInitialized) {
            Workspace.replaceImportWordLinks(cla)
        }
        cla.goToNextStep()
    }

    private var lastPosition = -1

    init {
        mContext = cla
        dataSetArray = data
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the data item for this position
        var convertView = convertView
        val dataModel = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        val viewHolder: ViewHolder // view lookup cache stored in tag
        val result: View?
        if (convertView == null) {
            viewHolder = ViewHolder()
            val inflater = LayoutInflater.from(context)
            convertView = inflater.inflate(R.layout.bloom_list_item, parent, false)
            viewHolder.chkItem =
                convertView.findViewById<View>(R.id.checkedTextView) as CheckedTextView
            result = convertView
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
            result = convertView
        }
        val animation = AnimationUtils.loadAnimation(
            mContext,
            if (position > lastPosition) R.anim.enter_up else R.anim.enter_down
        )
        result!!.startAnimation(animation)
        lastPosition = position
        val tmpString: String
        tmpString = dataModel!!.name
        viewHolder.chkItem!!.text = tmpString
        setCheckmark(viewHolder.chkItem, dataModel.getChecked())
        viewHolder.chkItem!!.setOnClickListener(this)
        viewHolder.chkItem!!.tag = position
        // Return the completed view to render on screen
        return convertView!!
    }

    fun setCheckmark(ctv: CheckedTextView?, checked: Boolean?) {
        ctv!!.isChecked = checked!!
        if (!ctv.isChecked) {
            ctv.setCompoundDrawables(null, null, null, null)
            //            ctv.setCheckMarkDrawable(null);
        } else {
            val img = context.resources.getDrawable(R.drawable.ic_checkmark_green)
            img.setBounds(0, 0, 120, 120)
            ctv.setCompoundDrawables(img, null, null, null)
            //            ctv.setCheckMarkDrawable(R.drawable.ic_checkmark_green);
        }
    }
}