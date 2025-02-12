package org.sil.storyproducer.controller

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.preference.PreferenceManager
import org.sil.storyproducer.R
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.model.Workspace
import java.util.Locale

class SplashScreenActivity : BaseActivity() {

    private lateinit var languageSpinner: Spinner
    private lateinit var sharedPreferences: SharedPreferences

    // Map of language names to locale codes
    private val languages = mapOf(
        "" to "",
        "English" to "en",
        "Français (French)" to "fr",
//        "Italiano (Italian)" to "it",
//        "Deutsch (German)" to "de",
        "Español (Spanish)" to "es",
//        "日本語 (Japanese)" to "ja",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get the shared preferences used by the Settings activity
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        if (Workspace.isInitialized) {
            showMain()
        } else {
            setContentView(R.layout.activity_splash_screen)

            try {
                val title: TextView = findViewById(R.id.version)
                title.text = packageManager.getPackageInfo(packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

            languageSpinner = findViewById(R.id.language_spinner)

            // get the current language settings
            val currentLocale = Locale.getDefault()
            val languageCode = currentLocale.language // e.g., "en", "es", "fr"
            val countryCode = currentLocale.country   // e.g., "US", "ES", "FR"

            // Set any saved language which takes precedence
            val savedLanguage = sharedPreferences.getString("language", "") ?: ""
            if (savedLanguage != "") {
                LanguageHelper.setLocale(applicationContext, savedLanguage)
                initWorkspace()
                return
            }

            // If system default language is supported then remember and use it
            if (languages.containsValue(languageCode)) {
                LanguageHelper.setLocale(applicationContext, languageCode)
                initWorkspace()
                return
            }

            // User needs to select a language so make the language selection spinner etc. visible
            languageSpinner.visibility = View.VISIBLE
            var languageIcon = findViewById<ImageView>(R.id.language_icon)
            languageIcon.visibility = View.VISIBLE
            var selectLang = findViewById<TextView>(R.id.select_lang)
            selectLang.visibility = View.VISIBLE

            // Populate Spinner
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages.keys.toList())
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            languageSpinner.adapter = adapter
//            languageSpinner.prompt = "Select a language"

            // Select empty language string in Spinner
            val currentLanguageIndex = languages.values.indexOf("")
            if (currentLanguageIndex != -1) {
                languageSpinner.setSelection(currentLanguageIndex)
            }

            // Handle language selection
            languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val selectedLanguageCode = languages.values.toList()[position]
                    if (selectedLanguageCode != "") {
                        LanguageHelper.setLocale(applicationContext, selectedLanguageCode)
                        initWorkspace()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }
}
