package org.tyndalebt.storyproduceradv.controller.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import org.tyndalebt.storyproduceradv.R

class PhaseSpinnerAdapter(context: Context, list : List<PhaseObject>)
    : ArrayAdapter<PhaseObject>(context, 0 , list)
{
    private var layoutInflater = LayoutInflater.from(context)

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
    {
        val view: View = layoutInflater.inflate(R.layout.phase_spinner_bg, null, true)
        return view(view, position)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View
    {
        var cv = convertView
        if(cv == null) {
            cv = layoutInflater.inflate(R.layout.phase_spinner_item, parent, false)
        }
        return view(cv!!, position)
    }

    // This routine is called when each view of the drop down list is created as well as the view that shows the spinner after a selection
    //  Consequently, depending on what layout the view is coming from, some of the calls to findViewByID will come back null and do nothing
    //  Note phasecolorBlob is in both the individual item as well as the spinner
    private fun view(view: View, position: Int): View
    {
        val phaseObject : PhaseObject = getItem(position) ?: return view

        val phaseNameItem = view.findViewById<TextView>(R.id.phaseName)
        val phaseNameBG = view.findViewById<TextView>(R.id.phaseNameBG)

        phaseNameBG?.text = phaseObject.name
        phaseNameBG?.setTextColor(Color.parseColor(phaseObject.contrastHexHash))

        phaseNameItem?.text = phaseObject.name
        phaseNameItem?.setBackgroundColor(ContextCompat.getColor(context, phaseObject.resIDHash))
        phaseNameItem?.setTextColor(Color.parseColor(phaseObject.contrastHexHash))

//        val relLayout = RelativeLayout.LayoutParams(100, 50)

        return view
    }
}













