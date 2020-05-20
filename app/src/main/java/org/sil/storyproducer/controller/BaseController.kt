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

        val storyDirectories = Workspace.storyDirectories()
        val storyBloomFiles = Workspace.storyBloomFiles()


        storyDirectories.forEach {
            Workspace.buildStory(context, it)?.also {
                Workspace.Stories.add(it)
            }
        }

        if (storyBloomFiles.size > 0) {
            updateStoriesAsync(storyDirectories.size, storyBloomFiles)
        } else {
            onStoriesUpdated()
        }
    }

    fun updateStoriesAsync(storyDirectoriesCount: Int, bloomFiles: List<DocumentFile>) {
        val current = storyDirectoriesCount + 1
        val total = storyDirectoriesCount + bloomFiles.size
        view.showReadingTemplatesDialog(this)
        updateStoryAsync(bloomFiles, 0, current, total)
    }

    fun updateStoryAsync(bloomFiles: List<DocumentFile>, index: Int, current: Int, total: Int) {
        val bloomFile = bloomFiles.get(index)
        view.updateReadingTemplatesDialog(current, total, bloomFile.name.orEmpty())
        subscriptions.add(
                Single.fromCallable {
                    Workspace.buildStory(context, bloomFile)?.also {
                        Workspace.Stories.add(it)
                    }
                }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ onUpdateStoryAsyncSuccess(bloomFiles, index, current, total) }, ::onUpdateStoryError)
        )
    }

    private fun onUpdateStoryAsyncSuccess(bloomFiles: List<DocumentFile>, index: Int, current: Int, total: Int) {
        val nextIndex = index + 1
        if (!cancelUpdate && nextIndex < bloomFiles.size) {
            updateStoryAsync(bloomFiles, nextIndex, current + 1, total)
        } else {
            onLastStoryUpdated()
        }
    }

    private fun onLastStoryUpdated() {
        Workspace.Stories.sortBy { it.title }
        Workspace.phases = Workspace.buildPhases()
        Workspace.activePhaseIndex = 0
        Workspace.updateStoryLocalCredits(context)

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

    private fun onUpdateStoryError(th: Throwable) {
        view.hideReadingTemplatesDialog()
        Timber.e(th)
    }

}