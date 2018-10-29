package org.sil.storyproducer.tools.file

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import org.sil.storyproducer.controller.KeyTermView
import org.sil.storyproducer.model.Workspace

fun stringToSpannableString(strings : MutableList<String>): SpannableStringBuilder {
    val newString = SpannableStringBuilder()
    for(text in strings){
        val tempString = SpannableString(text)
        if(Workspace.keytermsMap.containsKey(text)){
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(textView : View) {
                    //bundle up the key term to send to new fragment
                    val args = Bundle()
                    args.putParcelable("KeyTerm", Workspace.keytermsMap[text])
                    val fragment = KeyTermView()
                    fragment.arguments = args
                    //fragmentManager.beginTransaction().replace(R.id.frame_id, fragment).addToBackStack("").commit()
                }
            }
            tempString.setSpan(clickableSpan, 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            tempString.setSpan(ForegroundColorSpan(Color.BLUE), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        newString.append(tempString)
    }
    return newString
}