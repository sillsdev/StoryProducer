package org.tyndalebt.storyproduceradv.activities

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.fasterxml.jackson.databind.ObjectMapper
import dev.b3nedikt.restring.Restring
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.JsonHelper
import org.tyndalebt.storyproduceradv.controller.SplashScreenActivity
import org.tyndalebt.storyproduceradv.controller.adapter.ChooseLangAdapter
import org.tyndalebt.storyproduceradv.controller.adapter.DownloadDS
import org.tyndalebt.storyproduceradv.model.Workspace
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*

internal const val CHOOSE_LANGUAGE_FILE = "language.csv"

class ChooseLangActivity : BaseActivity() {
    var mDrawerLayout: DrawerLayout? = null
    var pView: ListView? = null
    var pDownloadImage: ImageView? = null
    var itemArray = arrayOf<String>()
    var tagArray = arrayOf<String>()
    private var bloomFileContents: String = ""
    var languageStringsMap: HashMap<String, String> = HashMap()

    var initialSetup: Boolean = false
    var chosenLanguage: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chosenLanguage = Workspace.readFromFile(this)
        if (chosenLanguage != "" && !Workspace.isInitialized) {
            setLanguage(chosenLanguage!!)
            goToNextStep()
        }
        else {
            setContentView(R.layout.activity_download)
            parseLangFile()

            buildLanguageList(itemArray, tagArray)
        }
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

    fun setLanguage(pChosenLanguage: String) {
        val langCode: String = Workspace.getLanguageCode(pChosenLanguage)
        if (langCode != "") {
            updateAppLanguage(langCode)
        }
        writeToFile(pChosenLanguage, this)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun updateStringsHashmap(language: String) {
        getLocalStringJsonHashmap(language).forEach {
            languageStringsMap[it.key] = it.value
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getLocalStringJsonHashmap(language: String): HashMap<String, String> {
        val listTypeJson: HashMap<String, String> = HashMap()
        var TmpStr: String = ""
        try {
            applicationContext.assets.open("$language/strings.json").use { inputStream ->
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
        updateStringsHashmap(language)
        Restring.locale = Locale(language)
        Restring.putStrings(Restring.locale, languageStringsMap)
    }

    public fun goToNextStep() {
        if (Workspace.isInitialized) {
            showMain()
        } else {
            startActivity(Intent(this, SplashScreenActivity::class.java))
            finish()
        }
    }

    private fun writeToFile(data: String, context: Context) {
        try {
            val outputStreamWriter =
                OutputStreamWriter(context.openFileOutput("config.txt", MODE_PRIVATE))
            outputStreamWriter.write(data)
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }
}
