package org.tyndalebt.storyproduceradv.controller.adapter

import androidx.core.content.res.ResourcesCompat

class PhaseObject(var name: String, var pResID: Int, var contrastHex: String)
{
    val resIDHash : Int = pResID
    val contrastHexHash : String = "#$contrastHex"
}