package org.sil.storyproducer.controller.bldownload

import android.net.Uri

class BLDataModel(var title: String,
                  var lang: String,
                  var imageId: Int,
                  var downloadUri: String,
                  var isInWorkspace: Boolean = false,
                  var isChecked: Boolean = false,
                  var downloadId: Long = -1
)
