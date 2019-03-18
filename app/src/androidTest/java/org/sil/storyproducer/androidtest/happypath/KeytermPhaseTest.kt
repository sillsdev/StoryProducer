package org.sil.storyproducer.androidtest.happypath

import android.graphics.drawable.Drawable
import android.support.constraint.ConstraintLayout
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.test.espresso.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator
import org.sil.storyproducer.model.Workspace

@LargeTest
@RunWith(AndroidJUnit4::class)
class KeytermPhaseTest : PhaseTestBase() {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.translate)
        pressKeyterm("God", R.id.fragment_scripture_text)
    }

    @Test
    fun should_BeAbleToNavigateToDeeperKeyterm() {
        pressKeyterm("idol", R.id.related_terms_text)

        var keytermTitle = (ActivityAccessor.getCurrentActivity() as AppCompatActivity).supportActionBar?.title
        Assert.assertEquals("Expected keyterm title text to be 'idol'", "idol", keytermTitle)
        Espresso.pressBack()
        //Need time for title to be updated
        Thread.sleep(Constants.durationToWaitWhenUIUpdates)
        keytermTitle = (ActivityAccessor.getCurrentActivity() as AppCompatActivity).supportActionBar?.title
        Assert.assertEquals("Expected keyterm title text to be 'God'", "God", keytermTitle)
    }

    @Test
    fun should_BeAbleToNavigateToKeytermFromList() {
        //Return to the translate phase
        Espresso.pressBack()
        Thread.sleep(Constants.durationToPlayTranslatedClip)
        pressKeytermInList("Abraham")
        val keytermTitle = (ActivityAccessor.getCurrentActivity() as AppCompatActivity).supportActionBar?.title
        Assert.assertEquals("Expected backtranslation box text to be 'Abraham'", "Abraham", keytermTitle)
    }

    @Test
    fun should_BeAbleToOpenCloseKeytermRecordingList() {
        recordAnAudioTranslationClip()
        pressRecordingListButton()
        val originalSheetState = BottomSheetBehavior.from(ActivityAccessor.getCurrentActivity()?.findViewById<ConstraintLayout>(org.sil.storyproducer.R.id.bottom_sheet)).state
        Assert.assertEquals("Expected the recording list state to be closed after recording and clicked", BottomSheetBehavior.STATE_COLLAPSED, originalSheetState)
        pressRecordingListButton()
        val finalSheetState = BottomSheetBehavior.from(ActivityAccessor.getCurrentActivity()?.findViewById<ConstraintLayout>(org.sil.storyproducer.R.id.bottom_sheet)).state
        Assert.assertNotEquals("Expected the recording list state to change", originalSheetState, finalSheetState)
    }

    @Test
    fun should_BeAbleToPlayRecordingOfAKeytermFromToolbar() {
        makeSureAnAudioClipIsAvailable()

        val originalIcon = getToolbarPlayIcon()
        pressPlayPauseButton()
        Thread.sleep(Constants.durationToWaitWhenUIUpdates)
        val endingIcon = getToolbarPlayIcon()
        Assert.assertNotEquals("Expected image to change.", originalIcon, endingIcon)
    }

    @Test
    fun should_BeAbleToPlayRecordingOfAKeytermFromList() {
        makeSureAnAudioClipIsAvailable()

        val originalIcon = getListPlayIcon()
        pressPlayPauseButtonInList()
        Thread.sleep(Constants.durationToWaitWhenUIUpdates)
        val endingIcon = getListPlayIcon()
        Assert.assertNotEquals("Expected image to change.", originalIcon, endingIcon)
    }

    @Test
    fun should_BeAbleToMakeKeytermRecording() {
        onView(withId(R.id.recordings_list)).check(RecyclerViewItemCountAssertion(0))
        recordAnAudioTranslationClip()
        onView(withId(R.id.recordings_list)).check(RecyclerViewItemCountAssertion(1))
    }

    @Test
    fun should_BeAbleToDeleteRecordingOfAKeytermFromList() {
        makeSureAnAudioClipIsAvailable()
        recordAnAudioTranslationClip()

        onView(withId(R.id.recordings_list)).check(RecyclerViewItemCountAssertion(2))
        pressDeleteButton()
        onView(withId(R.id.recordings_list)).check(RecyclerViewItemCountAssertion(1))
    }

    @Test
    fun should_BeAbleToAddBacktranslationText() {
        makeSureAnAudioClipIsAvailable()

        val originalText = Workspace.activeKeyterm.keytermRecordings[0].textBackTranslation
        onView(withId(R.id.backtranslation_edit_text)).check(matches(withText(originalText)))
        selectTextBox()
        typeTextInBox()
        submitTextToSave()
        val finalText = Workspace.activeKeyterm.keytermRecordings[0].textBackTranslation
        Assert.assertEquals("Expected backtranslation text to be 'Test'.", "Test", finalText)
        onView(withId(R.id.backtranslation_comment_title)).check(matches(withText(finalText)))
    }

    @Test
    fun should_BeAbleToDeleteBacktranslationText() {
        makeSureAnAudioClipIsAvailable()

        selectTextBox()
        typeTextInBox()
        submitTextToSave()
        val originalText = Workspace.activeKeyterm.keytermRecordings[0].textBackTranslation
        deleteBacktranslationText()
        val finalText = Workspace.activeKeyterm.keytermRecordings[0].textBackTranslation
        Assert.assertNotEquals("Expected backtranslation to be empty.", originalText, finalText)
        onView(withId(R.id.backtranslation_edit_text)).check(matches(withText("")))
    }

    private fun makeSureAnAudioClipIsAvailable() {
        if (!areThereAnyAudioClipsOnThisSlide()) {
            recordAnAudioTranslationClip()
        }
    }

    private fun getToolbarPlayIcon(): Drawable {
        return ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(org.sil.storyproducer.R.id.play_recording_button)!!.background.current
    }

    private fun getListPlayIcon(): Drawable {
        return ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(org.sil.storyproducer.R.id.audio_comment_play_button)!!.drawable.current
    }

    private fun pressPlayPauseButton() {
        onView(allOf(withId(org.sil.storyproducer.R.id.play_recording_button), isDisplayed())).perform(click())
    }

    private fun pressPlayPauseButtonInList() {
        onView(allOf(withId(org.sil.storyproducer.R.id.audio_comment_play_button), isDisplayed())).perform(click())
    }

    private fun pressDeleteButton() {
        onView(allOf(withParent(withChild(ViewMatchers.withText("God_2"))), withId(R.id.audio_comment_delete_button), isDisplayed())).perform(click())
        onView(allOf(withText(R.string.yes), isDisplayed())).perform(click())
    }

    private fun selectTextBox() {
        onView(allOf(withId(org.sil.storyproducer.R.id.backtranslation_edit_text), isDisplayed())).perform(click())
    }

    private fun typeTextInBox() {
        onView(allOf(withId(org.sil.storyproducer.R.id.backtranslation_edit_text), isDisplayed())).perform(ViewActions.typeText("Test"))
    }

    private fun submitTextToSave() {
        onView(allOf(withId(org.sil.storyproducer.R.id.submit_backtranslation_button), isDisplayed())).perform(click())
    }

    private fun deleteBacktranslationText() {
        onView(allOf(withId(org.sil.storyproducer.R.id.backtranslation_comment_delete_button), isDisplayed())).perform(click())
    }

    private fun pressRecordingListButton() {
        onView(allOf(withId(org.sil.storyproducer.R.id.list_recordings_button), isDisplayed())).perform(click())
    }

    /**
     * @param keyterm keyterm to look for
     * @param id view keyterm is in
     */
    private fun pressKeyterm(keyterm: String, id: Int) {
        onView(allOf(withId(id), isDisplayed())).perform(clickClickableSpan(keyterm))
    }

    private fun pressKeytermInList(keyterm: String){
        onView(allOf(withContentDescription(R.string.nav_open), isDisplayed())).perform(click())
        onView(allOf(withText(R.string.title_activity_keyterm_list), isDisplayed())).perform(click())
        onView(allOf(withText(keyterm), isDisplayed())).perform(click())
    }

    private fun clickClickableSpan(s: String): ViewAction? {
        return object : ViewAction{
            override fun getDescription(): String {
                return "Clicking on a clickableSpan"
            }

            override fun getConstraints(): Matcher<View> {
                return Matchers.instanceOf(TextView::class.java)
            }

            override fun perform(uiController: UiController?, view: View?) {
                val textView = view as TextView
                val spannableString = textView.text as SpannableString

                if (spannableString.isEmpty()) {
                    // TextView is empty, nothing to do
                    throw NoMatchingViewException.Builder()
                            .includeViewHierarchy(true)
                            .withRootView(textView)
                            .build()
                }

                // Get the links inside the TextView and check if we find textToClick
                val spans = spannableString.getSpans(0, spannableString.length, ClickableSpan::class.java)
                if (spans.isNotEmpty()) {
                    var spanCandidate: ClickableSpan
                    for (span in spans) {
                        spanCandidate = span
                        val start = spannableString.getSpanStart(spanCandidate)
                        val end = spannableString.getSpanEnd(spanCandidate)
                        val sequence = spannableString.subSequence(start, end)
                        if (s == sequence.toString()) {
                            span.onClick(textView)
                            return
                        }
                    }
                }
            }

        }
    }
    inner class RecyclerViewItemCountAssertion(private var expectedCount: Int) : ViewAssertion {
        override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
            if(noViewFoundException != null){
                throw noViewFoundException
            }

            val recyclerView: RecyclerView = view as RecyclerView
            val adapter = recyclerView.adapter
            assertThat(adapter?.itemCount, `is`(expectedCount))
        }
    }
}
