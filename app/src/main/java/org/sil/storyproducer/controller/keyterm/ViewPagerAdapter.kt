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
                val keyterm_text = keyterm_text()
                keyterm_text.arguments = bundle
                return keyterm_text
            }
            1 -> {
                return keyterm_audio()
            }
        }
        return null
    }

    override fun getCount(): Int {
        return 2
    }
}