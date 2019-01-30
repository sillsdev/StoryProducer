package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

class ViewPagerAdapter(fm:FragmentManager, private val clickedTerm: String) : FragmentPagerAdapter(fm) {
    override fun getItem(p0: Int): Fragment? {

        when(p0){
            0 -> {
                val keytermText = KeyTermTextFrag()
                val bundle = Bundle()
                bundle.putString("ClickedTerm", clickedTerm)
                keytermText.arguments = bundle
                return keytermText
            }
            1 -> return KeyTermAudioFrag()
        }
        return null
    }

    override fun getCount(): Int {
        return 2
    }
}
