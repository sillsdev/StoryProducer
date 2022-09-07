package org.tyndalebt.storyproduceradv.controller.adapter

import android.os.Bundle
import android.provider.Settings.Global.getString
import android.provider.Settings.Secure.getString
import android.provider.Settings.System.getString
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.TypedArrayUtils.getString
import org.tyndalebt.storyproduceradv.model.Workspace

class PhaseList
{
    private val blackHex = "000000"
    private val whiteHex = "FFFFFF"

//    val defaultPhase: PhaseObject = phases()[0]
    val phases = ArrayList<PhaseObject>()

    public fun getPhaseList() : ArrayList<PhaseObject>
    {
        if (phases.size > 0) return phases
        var idx = 0
        while (idx < Workspace.phases.size) {
            phases.add(PhaseObject(Workspace.phases[idx].getDisplayName(), Workspace.phases[idx].getColor(), "FFFFFF"))
            idx += 1
        }
        return phases
    }
/*
    fun phasePosition(phaseObject: PhaseObject): Int
    {
        for (i in phases.iterator())
        {
            if(phaseObject == phases[i])
                return i
        }
        return 0
    }
 */
}

