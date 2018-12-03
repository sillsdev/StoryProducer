package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import org.sil.storyproducer.model.Keyterm

class ViewPagerAdapter(fm:FragmentManager, val keyterm: Keyterm) : FragmentPagerAdapter(fm) {
    override fun getItem(p0: Int): Fragment? {
        val bundle = Bundle()
        bundle.putParcelable("Keyterm", keyterm)

        when(p0){
            0 -> {
                val keyterm_audio = KeyTermAudioFrag()
                keyterm_audio.arguments = bundle
                return keyterm_audio

            }
            1 -> {
                val keyterm_text = KeyTermTextFrag()
                keyterm_text.arguments = bundle
                return keyterm_text
            }
        }
        return null
    }

    override fun getCount(): Int {
        return 2
    }
}