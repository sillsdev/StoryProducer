package org.sil.storyproducer.controller

import android.content.Context
import androidx.documentfile.provider.DocumentFile
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
    private var cancelUpdate = false

    fun cancelUpdate() {
        cancelUpdate = true
        view.showCancellingReadingTemplatesDialog()
    }

    fun updateStories() {
        Workspace.Stories.clear()
        val storyPaths = Workspace.storyPaths()
        if (storyPaths.size > 0) {
            view.showReadingTemplatesDialog(this)
            updateStory(storyPaths, 0)
        }
    }

    fun updateStory(storyPaths: List<DocumentFile>, index: Int) {
        val storyPath = storyPaths.get(index)
        view.updateReadingTemplatesDialog(index + 1, storyPaths.size, storyPath.name.orEmpty())
        subscriptions.add(
                Single.fromCallable {
                    Workspace.buildStory(context, storyPath)?.also {
                        Workspace.Stories.add(it)
                    }
                }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ onUpdateStorySuccess(storyPaths, index) }, ::onUpdateStoryError)
        )
    }

    private fun onUpdateStorySuccess(storyPaths: List<DocumentFile>, index: Int) {
        val nextIndex = index + 1
        if (!cancelUpdate && nextIndex < storyPaths.size) {
            updateStory(storyPaths, nextIndex)
        } else {
            onStoriesUpdated()
        }
    }

    private fun onStoriesUpdated() {
        Workspace.Stories.sortBy { it.title }
        Workspace.phases = Workspace.buildPhases()
        Workspace.activePhaseIndex = 0
        Workspace.updateStoryLocalCredits(context)

        view.hideReadingTemplatesDialog()
        if (Workspace.registration.complete) {
            view.showMain()
        } else {
            view.showRegistration()
        }
    }

    private fun onUpdateStoryError(th: Throwable) {
        view.hideReadingTemplatesDialog()
        Timber.e(th)
    }

}