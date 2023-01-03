package org.tyndalebt.storyproduceradv.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.fasterxml.jackson.databind.ObjectMapper
import dev.b3nedikt.restring.Restring
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.JsonHelper
import org.tyndalebt.storyproduceradv.controller.MainActivity
import org.tyndalebt.storyproduceradv.controller.adapter.ChooseLangAdapter
import org.tyndalebt.storyproduceradv.controller.adapter.DownloadDS
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.tools.file.goToURL
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

internal const val CHOOSE_LANGUAGE_FILE = "language.csv"

class ChooseLangActivity : BaseActivity() {
    var mDrawerLayout: DrawerLayout? = null
    var pView: ListView? = null
    var pDownloadImage: ImageView? = null
    var itemArray = arrayOf<String>()
    var tagArray = arrayOf<String>()
    private var bloomFileContents: String = ""
    var englishStringsMap: HashMap<String, String> = HashMap()
    var spanishStringsMap: HashMap<String, String> = HashMap()
    var swahiliStringsMap: HashMap<String, String> = HashMap()
    var tokpisinStringsMap: HashMap<String, String> = HashMap()
    var portugueseStringsMap: HashMap<String, String> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

//        var itemArray = arrayOf<String>("Surat","Mumbai","Rajkot")
//        var tagArray = arrayOf<String>("C","B","C")
        parseLangFile()

        buildLanguageList(itemArray, tagArray)
    }

    fun buildLanguageList(pList: Array<String>, pURL: Array<String>) {
        setContentView(R.layout.bloom_list_container)

        val mActionBarToolbar = findViewById<Toolbar>(R.id.toolbarMoreTemplates)
        mActionBarToolbar.visibility = View.INVISIBLE

        mDrawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout_bloom)
        //Lock from opening with left swipe
        //Lock from opening with left swipe
        mDrawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        pView = findViewById<View>(R.id.bloom_list_view) as ListView
        pDownloadImage = findViewById<View>(R.id.image_download) as ImageView

        val arrayList = ArrayList<DownloadDS?>()
        var idx: Int
        var tmp: String?

        idx = 0
        while (idx < pList.size) {
            arrayList.add(DownloadDS(pList[idx], pURL[idx], false))
            idx++
        }

        val arrayAdapter = ChooseLangAdapter(arrayList, this)
        pView!!.setAdapter(arrayAdapter)

        pDownloadImage!!.setImageBitmap(BitmapFactory.decodeResource(this.resources, R.drawable.language))
    }

    fun parseLangFile(): Boolean {
        var i: Int
        var result = ""

        var fis: InputStream? = null
        try {
            val sourceFile = CHOOSE_LANGUAGE_FILE
            //fis = FileInputStream(sourceFile)
            fis = assets.open(CHOOSE_LANGUAGE_FILE)
            var current: Char
            while (fis.available() > 0) {
                current = fis.read().toChar()
                result = result + current.toString()
            }
        } catch (e: Exception) {
            Log.d("ChooseLangActivity:parseLangFile", e.toString())
            val mDisplayAlert = Intent(this, DisplayAlert::class.java)
            mDisplayAlert.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mDisplayAlert.putExtra("title", getString(R.string.more_templates))
            mDisplayAlert.putExtra(
                "body",
                getString(R.string.remote_check_msg_no_connection)
            )
            startActivity(mDisplayAlert)
            return false
        }
        bloomFileContents = result

        //val lines = result.split("\\r\\n")
        val lines = result.lines()
        var itemString = ""
        var tagString = ""
        var idx: Int
        idx = 0
        while (idx < lines.size) {
            val lang = lines[idx].split(",").toTypedArray()
            if (itemString != "") {
                itemString = "$itemString|"
                tagString = "$tagString|"
            }
            val buffer = StandardCharsets.ISO_8859_1.encode(lang[1])
            val encodedString = StandardCharsets.UTF_8.decode(buffer).toString()

            itemString = itemString + encodedString
            tagString = tagString + lang[0]
            idx++
        }

        itemArray = itemString.split("|").toTypedArray()
        tagArray = tagString.split("|").toTypedArray()
        return true
    }

    public fun setLanguage(pChosenLanguage: String) {
        updateStringsHashmap()
        if (pChosenLanguage == "Bislama") {
        } else if (pChosenLanguage == "French") {
        } else if (pChosenLanguage == "Indonesian") {
        } else if (pChosenLanguage == "Khmer") {
        } else if (pChosenLanguage == "Portuguese") {
            updateAppLanguage("por")
        } else if (pChosenLanguage == "Spanish") {
            updateAppLanguage("es")
        } else if (pChosenLanguage == "Swahili") {
            updateAppLanguage("sw")
        } else if (pChosenLanguage == "Tok Pisin") {
            updateAppLanguage("tkp")
        } else if (pChosenLanguage == "") {
        }
        else {   // English or not defined
            updateAppLanguage("en")
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun updateStringsHashmap() {
        getLocalStringJsonHashmap("en").forEach {
            englishStringsMap[it.key] = it.value
        }
        getLocalStringJsonHashmap("es").forEach {
            spanishStringsMap[it.key] = it.value
        }
        getLocalStringJsonHashmap("sw").forEach {
            swahiliStringsMap[it.key] = it.value
        }
        getLocalStringJsonHashmap("tkp").forEach {
            tokpisinStringsMap[it.key] = it.value
        }
        getLocalStringJsonHashmap("por").forEach {
            portugueseStringsMap[it.key] = it.value
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getLocalStringJsonHashmap(language: String): HashMap<String, String> {
        val listTypeJson: HashMap<String, String> = HashMap()
        var TmpStr: String = ""
        try {
            applicationContext.assets.open("$language.json").use { inputStream ->
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                val jsonString = String(buffer, StandardCharsets.UTF_8)
                JsonHelper().getFlattenedHashmapFromJsonForLocalization(
                    "",
                    ObjectMapper().readTree(jsonString),
                    listTypeJson
                )
            }
        } catch (exception: IOException) {
            TmpStr = exception.localizedMessage
        }
        return listTypeJson
    }

    fun updateAppLanguage(language: String) {
        Restring.locale = Locale(language)
        if (language == "es") {
            Restring.putStrings(Restring.locale, spanishStringsMap)
        } else if (language == "sw") {
            Restring.putStrings(Restring.locale, swahiliStringsMap)
        } else if (language == "tkp") {
            Restring.putStrings(Restring.locale, tokpisinStringsMap)
        } else if (language == "por") {
            Restring.putStrings(Restring.locale, portugueseStringsMap)
        } else {
            Restring.locale = Locale("en") // in case its not set
            Restring.putStrings(Restring.locale, englishStringsMap)
        }
//        var StringsArrayMap: HashMap<String, Array<CharSequence>> = HashMap()
//        Restring.putStringArrays(Restring.locale, StringsArrayMap)
    }

}
