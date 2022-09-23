package org.sil.storyproducer.controller.bldownload

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.cardview.widget.CardView

// This class extends the android CardView widget and implements a Checkable interface
// It is used to display a single Bloom Library downloadable story with a checked tick icon,
// if the story item has been selected for download.
class BLCheckableCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CardView(context, attrs), Checkable {

    var mIsChecked: Boolean = false

    override fun performClick(): Boolean {
        toggle()
        return super.performClick()
    }

    override fun setChecked(checked: Boolean) {
        mIsChecked = checked
    }

    override fun isChecked(): Boolean {
        return mIsChecked
    }

    override fun toggle() {
        mIsChecked = !this.mIsChecked
    }
}
