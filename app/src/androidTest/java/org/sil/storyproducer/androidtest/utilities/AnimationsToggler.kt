package org.sil.storyproducer.androidtest.utilities

import android.app.Activity
import android.preference.PreferenceManager
import org.sil.storyproducer.film.R

object AnimationsToggler {

    private fun enableCustomAnimations() {
        val activity = ActivityAccessor.getCurrentActivity()
        val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(activity).edit()
        preferencesEditor.remove(activity!!.resources.getString(R.string.recording_toolbar_disable_animation))
        preferencesEditor.commit()
    }

    private fun disableCustomAnimations() {
        val activity = ActivityAccessor.getCurrentActivity()
        val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(activity).edit()
        preferencesEditor.putBoolean(activity!!.resources.getString(R.string.recording_toolbar_disable_animation), true)
        preferencesEditor.commit()
    }

    fun withoutCustomAnimations(function: () -> Unit) {
        try {
            disableCustomAnimations()
            function()
        }
        finally {
            enableCustomAnimations()
        }
    }
}