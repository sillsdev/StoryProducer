package org.tyndalebt.storyproduceradv.activities

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import org.tyndalebt.storyproduceradv.R

class DisplayAlert : AppCompatActivityMTT() {
    internal var title: String? = null
    internal var body: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = intent.extras
        if (b != null) {
            title = b!!.getString("title")
            body = b!!.getString("body")
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setIcon(R.drawable.ic_smartphone_white_24dp)
            builder.setMessage(body)
                .setCancelable(false)
                .setPositiveButton("Ok") { dialog, id ->
                    finish()
                }
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }
    companion object {
        var b: Bundle? = null
    }
}