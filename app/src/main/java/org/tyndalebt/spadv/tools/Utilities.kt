package org.tyndalebt.spadv.tools

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.media.MediaCodecList
import android.media.MediaCodecInfo
import android.os.Build


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

fun selectCodec(mimeType: String): MediaCodecInfo? {
    // 3/30/2022 - DKH, Issue 630: Audio and slides are out of sync
    // During Android 12 testing, the encoding of the audio to the slides was out of sync, ie,
    // the slide video finished earlier than the audio track.  This produced a video with the
    // audio being played over the wrong slide.
    //
    // The origin code assumed the encoder for a codec mime type (eg, "video/avc") would always
    // properly encode the video.  As it turns out, there can be more than one encoder for
    // a mime type, so picking the first encoder in the list may not produce the desired
    // video.  The code was updated to look for a codec encoder with a specific canonical name that
    // has always worked for us in the past.  If we don't find that canonical name, just use
    // the first codec encoder associated with mime (the way the original code worked).
    //
    // For the "video/avd" codec mime,  we look for a specific canonical name.
    val myVideoAvcMime = "video/avc"  // For this mime we need a specific encoder
    val myVideoAvcCanonicalName = "c2.android.avc.encoder"  // Encoder needed for "video/avc"

    // For the audio/mp4a-latm codec mime, we look for a specific canonical name.
    val myAudioMp4Mime = "audio/mp4a-latm"  // For this mime we need a specific encoder
    val myAudioMp4CanonicalName = "c2.android.aac.encoder"  // Encoder needed for "audio/mp4a-latm"

    // If we don't find the codec Encoder's canonical name we are looking for, record the
    // first encoder of the requested mimeType and use that mimeType/canonicalName encoder as default
    // Depending on the encoder, the video may not work properly
    var myDefaultCodecInfo: MediaCodecInfo? = null  // default encoder

    for (codecInfo in MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos) { // grab all codex

        if (!codecInfo.isEncoder) {// If this is not a codec encoder, get the next codec
            continue
        }

        val types = codecInfo.supportedTypes
        for (j in types.indices) {
            if (types[j].equals(mimeType, ignoreCase = true)) { // look for specific mime
                if (Build.VERSION.SDK_INT >= 29) { // canonicalName is only supported in sdk 29 or later
                    // We look for canonicalName, if we can't find it, we return the first encoder
                    // associated with the mime type
                    if(myDefaultCodecInfo == null) myDefaultCodecInfo = codecInfo // first encoder in mime list

                    when(mimeType){
                        myVideoAvcMime -> {  // this mime needs a specific canonical name
                            if (codecInfo.canonicalName.equals(myVideoAvcCanonicalName)) {
                                return codecInfo  // found the proper canonicalName encoder for this mime
                            }
                        }

                        myAudioMp4Mime->{ // this mime needs a specific canonical name
                            if (codecInfo.canonicalName.equals(myAudioMp4CanonicalName)) {
                                return codecInfo  // found the proper canonicalName encoder for this mime
                            }
                        }
                        else -> {
                            // This mime does not need a specific encoder with a canonical name, so use the first encoder
                            // associated with the mime type
                            return codecInfo
                        }
                    }

                }else{
                    // The canonicalName capability is not available, so,
                    // just return the first encoder associated with the mime type
                    return codecInfo
                }
            }
        }
    }
    // this could be null or the first encoder associated with a mime type that needed a
    // canonical name but the canonical name was not found
    return myDefaultCodecInfo
}