package org.sil.storyproducer.controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import org.sil.storyproducer.App
import org.sil.storyproducer.R
import org.sil.storyproducer.activities.BaseActivity

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        // restarts the settings activity
        private fun restartActivity(activity: Activity) {
            val intent = activity.intent
            activity.finish()
            activity.startActivity(intent)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val languagePreference = findPreference<ListPreference>("language") // get saved language setting

            // callback for user selecting a new language from settings activity
            languagePreference?.setOnPreferenceChangeListener { _, newValue ->
                val selectedLanguage = newValue as String
                if (App.languageCode != selectedLanguage) {
                    // process and save the new UI language
                    LanguageHelper.setLocale(requireActivity().applicationContext, selectedLanguage)

                    // Restart settings activity to apply changes
                    restartActivity(requireActivity()) // Call this from a fragment
                }

                true
            }
        }
    }

    // helper to restart SP app on language settings change
    private fun restartMainActivity(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // restart SP on settings back button pressed
        restartMainActivity(applicationContext)
    }

}