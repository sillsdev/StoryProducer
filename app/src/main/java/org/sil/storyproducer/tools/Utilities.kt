package org.sil.storyproducer.tools

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.Workspace

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun dpToPx(dp: Int, activity: Activity): Int{
    val metrics = DisplayMetrics()
    activity.windowManager.defaultDisplay.getMetrics(metrics)
    val logicalDensity = metrics.density
    return (dp * logicalDensity).toInt()
}

fun String.stripForFilename(): String {
    return this.replace("[^\\w -]".toRegex(), "_")
}

fun helpDialog(context: Context, title: String): AlertDialog.Builder {
    val alert = AlertDialog.Builder(context)
    alert.setTitle(title)

    val wv = WebView(context)
    val iStream = context.assets.open(Phase.getHelpName(Workspace.activePhase.phaseType))
    val text = iStream.reader().use {
        it.readText()
    }

    wv.loadData(text, "text/html", null)
    alert.setView(wv)
    alert.setNegativeButton("Close") { dialog, _ ->
        dialog!!.dismiss()
    }
    return alert
}

fun selectCodec(mimeType: String): MediaCodecInfo? {
    for (codecInfo in MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos) {
        if (!codecInfo.isEncoder) {
            continue
        }

        val types = codecInfo.supportedTypes
        for (j in types.indices) {
            if (types[j].equals(mimeType, ignoreCase = true)) {
                return codecInfo
            }
        }
    }
    return null
}