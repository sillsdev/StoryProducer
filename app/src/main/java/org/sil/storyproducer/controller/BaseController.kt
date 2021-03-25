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

        val storyFiles = Workspace.storyFiles()

        if (storyFiles.size > 0) {
            updateStoriesAsync(1, storyFiles.size, storyFiles)
        } else {
            onStoriesUpdated()
        }
    }

    fun updateStoriesAsync(current: Int, total: Int, files: List<DocumentFile>) {
        view.showReadingTemplatesDialog(this)
        updateStoryAsync(files, 0, current, total)
    }

    fun updateStoryAsync(files: List<DocumentFile>, index: Int, current: Int, total: Int) {
        val file = files.get(index)
        view.updateReadingTemplatesDialog(current, total, file.name.orEmpty())
        subscriptions.add(
                Single.fromCallable {
                    Workspace.buildStory(context, file)?.also {
                        Workspace.Stories.add(it)
                    }
                }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ onUpdateStoryAsync(files, index, current, total) }, { onUpdateStoryAsyncError(it, files, index, current, total) })
        )
    }

    private fun onUpdateStoryAsync(files: List<DocumentFile>, index: Int, current: Int, total: Int) {
        val nextIndex = index + 1
        if (!cancelUpdate && nextIndex < files.size) {
            updateStoryAsync(files, nextIndex, current + 1, total)
        } else {
            onLastStoryUpdated()
        }
    }

    private fun onUpdateStoryAsyncError(th: Throwable, files: List<DocumentFile>, index: Int, current: Int, total: Int) {
        Timber.e(th)
        onUpdateStoryAsync(files, index, current, total)
    }

    private fun onLastStoryUpdated() {
        Workspace.sortStoriesByTitle()
        Workspace.activePhaseIndex = 0

        view.hideReadingTemplatesDialog()
        onStoriesUpdated()
    }

    private fun onStoriesUpdated() {
        if (Workspace.registration.complete) {
            view.showMain()
        } else {
            view.showRegistration()
        }
    }

}