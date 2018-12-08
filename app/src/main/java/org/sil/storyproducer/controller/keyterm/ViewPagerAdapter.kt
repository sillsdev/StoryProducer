package org.sil.storyproducer.controller.keyterm

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

class ViewPagerAdapter(fm:FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getItem(p0: Int): Fragment? {

        when(p0){
            0 -> {
                val keyterm_audio = KeyTermAudioFrag()
                return keyterm_audio

            }
            1 -> {
                val keyterm_text = KeyTermTextFrag()
                return keyterm_text
            }
        }
        return null
    }

    override fun getCount(): Int {
        return 2
    }
}