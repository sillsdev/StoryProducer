package org.sil.storyproducer.controller

import android.content.Context
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.view.BaseActivityView
import timber.log.Timber

open class BaseController(
        val view: BaseActivityView,
        val context: Context
) {

    protected val subscriptions = CompositeDisposable()

    fun updateStories() {
        view.showReadingTemplatesDialog()
        subscriptions.add(
                Single.fromCallable { Workspace.updateStories(context) }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ onUpdateStoriesSuccess() }, ::onUpdateStoriesError)
        )
    }

    private fun onUpdateStoriesSuccess() {
        view.hideReadingTemplatesDialog()
        if (Workspace.registration.complete) {
            view.showMain()
        } else {
            view.showRegistration()
        }
    }

    private fun onUpdateStoriesError(th: Throwable) {
        view.hideReadingTemplatesDialog()
        Timber.e(th)
    }

}