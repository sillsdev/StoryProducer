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
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.AnimationsToggler
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
        Assert.assertEquals("Expected backtranslation box text to be 'idol'", "idol", keytermTitle)
        Espresso.pressBack()
        //Need time for title to be updated
        Thread.sleep(Constants.durationToPlayTranslatedClip)
        keytermTitle = (ActivityAccessor.getCurrentActivity() as AppCompatActivity).supportActionBar?.title
        Assert.assertEquals("Expected backtranslation box text to be 'God'", "God", keytermTitle)
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
    fun should_BeAbleToMakeKeytermRecording() {
        val originalNumberOfRecordings = getCurrentNumberOfRecordings()
        recordAnAudioTranslationClip()
        val finalNumberOfRecordings = getCurrentNumberOfRecordings()
        Assert.assertNotEquals("Expected number of recordings to increase.", originalNumberOfRecordings, finalNumberOfRecordings)
    }

    @Test
    fun should_BeAbleToOpenCloseKeytermRecordingList() {
        if (!areThereAnyAudioClipsOnThisSlide()) {
            recordAnAudioTranslationClip()
            pressRecordingListButton()
        }
        val originalSheetState = BottomSheetBehavior.from(ActivityAccessor.getCurrentActivity()?.findViewById<ConstraintLayout>(org.sil.storyproducer.R.id.bottom_sheet)).state
        pressRecordingListButton()
        val finalSheetState = BottomSheetBehavior.from(ActivityAccessor.getCurrentActivity()?.findViewById<ConstraintLayout>(org.sil.storyproducer.R.id.bottom_sheet)).state
        Assert.assertNotEquals("Expected the recording list state to change", originalSheetState, finalSheetState)
    }

    @Test
    fun should_BeAbleToPlayRecordingOfAKeytermFromToolbar() {
        makeSureAnAudioClipIsAvailable()

        val originalIcon = getToolbarPlayIcon()
        pressPlayPauseButton()
        giveAppTimeToPlayAudio()
        val endingIcon = getToolbarPlayIcon()
        Assert.assertNotEquals("Expected image to change.", originalIcon, endingIcon)
    }

    @Test
    fun should_BeAbleToPlayRecordingOfAKeytermFromList() {
        makeSureAnAudioClipIsAvailable()

        val originalIcon = getListPlayIcon()
        pressPlayPauseButtonInList()
        giveAppTimeToPlayAudio()
        val endingIcon = getListPlayIcon()
        Assert.assertNotEquals("Expected image to change.", originalIcon, endingIcon)
    }

    @Test
    fun should_BeAbleToDeleteRecordingOfAKeytermFromList() {
        makeSureAnAudioClipIsAvailable()

        val originalNumberOfRecordings = getCurrentNumberOfRecordings()
        pressDeleteButton()
        val finalNumberOfRecordings = getCurrentNumberOfRecordings()
        if(finalNumberOfRecordings != null) {
            Assert.assertEquals("Expected one less recordings to exist", originalNumberOfRecordings, finalNumberOfRecordings+1)
        }
        else{
            Assert.assertNull("Expected no recording to exist", finalNumberOfRecordings)
        }
    }

    @Test
    fun should_BeAbleToAddBacktranslationText() {
        makeSureAnAudioClipIsAvailable()

        val originalText = Workspace.activeKeyterm.keytermRecordings[0].textBackTranslation
        selectTextBox()
        typeTextInBox()
        submitTextToSave()
        val finalText = Workspace.activeKeyterm.keytermRecordings[0].textBackTranslation
        Assert.assertNotEquals("Expected backtranslation text to be 'Test'.", originalText, finalText)
        val backTranslationTextBoxText = ActivityAccessor.getCurrentActivity()?.findViewById<TextView>(org.sil.storyproducer.R.id.backtranslation_comment_title)?.text.toString()
        Assert.assertEquals("Expected backtranslation box text to be 'Test'", "Test", backTranslationTextBoxText)
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
        val backTranslationTextBoxText = ActivityAccessor.getCurrentActivity()?.findViewById<TextView>(org.sil.storyproducer.R.id.backtranslation_edit_text)?.text.toString()
        Assert.assertEquals("Expected backtranslation box to be empty", "", backTranslationTextBoxText)
    }

    private fun getCurrentNumberOfRecordings() =
            ActivityAccessor.getCurrentActivity()?.findViewById<RecyclerView>(org.sil.storyproducer.R.id.recordings_list)?.adapter?.itemCount

    private fun makeSureAnAudioClipIsAvailable() {
        if (!areThereAnyAudioClipsOnThisSlide()) {
            recordAnAudioTranslationClip()
        }
    }

    private fun recordAnAudioTranslationClip() {
        AnimationsToggler.withoutCustomAnimations {
            pressMicButton()
            giveAppTimeToRecordAudio()
            pressMicButton()
        }
    }

    private fun areThereAnyAudioClipsOnThisSlide(): Boolean {
        val showRecordingsListButton = ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(org.sil.storyproducer.R.id.list_recordings_button)
        return showRecordingsListButton?.visibility != View.INVISIBLE
    }

    private fun pressMicButton() {
        onView(CoreMatchers.allOf(withId(org.sil.storyproducer.R.id.start_recording_button), ViewMatchers.isDisplayed())).perform(click())
    }

    private fun giveAppTimeToRecordAudio() {
        Thread.sleep(Constants.durationToRecordTranslatedClip)
    }

    private fun getToolbarPlayIcon(): Drawable {
        return ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(org.sil.storyproducer.R.id.play_recording_button)!!.background.current
    }

    private fun getListPlayIcon(): Drawable {
        return ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(org.sil.storyproducer.R.id.audio_comment_play_button)!!.drawable.current
    }

    private fun pressPlayPauseButton() {
        onView(CoreMatchers.allOf(withId(org.sil.storyproducer.R.id.play_recording_button), ViewMatchers.isDisplayed())).perform(click())
    }

    private fun pressPlayPauseButtonInList() {
        onView(CoreMatchers.allOf(withId(org.sil.storyproducer.R.id.audio_comment_play_button), ViewMatchers.isDisplayed())).perform(click())
    }

    private fun pressDeleteButton() {
        onView(CoreMatchers.allOf(withId(org.sil.storyproducer.R.id.audio_comment_delete_button), ViewMatchers.isDisplayed())).perform(click())
    }

    private fun selectTextBox() {
        onView(CoreMatchers.allOf(withId(org.sil.storyproducer.R.id.backtranslation_edit_text), ViewMatchers.isDisplayed())).perform(click())
    }

    private fun typeTextInBox() {
        onView(CoreMatchers.allOf(withId(org.sil.storyproducer.R.id.backtranslation_edit_text), ViewMatchers.isDisplayed())).perform(ViewActions.typeText("Test"))
    }

    private fun submitTextToSave() {
        onView(CoreMatchers.allOf(withId(org.sil.storyproducer.R.id.submit_backtranslation_button), ViewMatchers.isDisplayed())).perform(click())
    }

    private fun deleteBacktranslationText() {
        onView(CoreMatchers.allOf(withId(org.sil.storyproducer.R.id.backtranslation_comment_delete_button), ViewMatchers.isDisplayed())).perform(click())
    }

    private fun pressRecordingListButton() {
        onView(CoreMatchers.allOf(withId(org.sil.storyproducer.R.id.list_recordings_button), ViewMatchers.isDisplayed())).perform(click())
    }

    /**
     * @param keyterm keyterm to look for
     * @param id view keyterm is in
     */
    private fun pressKeyterm(keyterm: String, id: Int) {
        onView(CoreMatchers.allOf(withId(id), ViewMatchers.isDisplayed())).perform(clickClickableSpan(keyterm))
    }

    private fun pressKeytermInList(keyterm: String){
        onView(ViewMatchers.withContentDescription(R.string.nav_open)).perform(click())
        onView(ViewMatchers.withText(R.string.title_activity_keyterm_list)).perform(click())
        onView(ViewMatchers.withText(keyterm)).perform(click())
    }

    private fun giveAppTimeToPlayAudio() {
        Thread.sleep(Constants.durationToPlayTranslatedClip)
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
                if (spans.size > 0) {
                    var spanCandidate: ClickableSpan
                    for (span in spans) {
                        spanCandidate = span
                        val start = spannableString.getSpanStart(spanCandidate)
                        val end = spannableString.getSpanEnd(spanCandidate)
                        val sequence = spannableString.subSequence(start, end)
                        if (s.toString().equals(sequence.toString())) {
                            span.onClick(textView)
                            return
                        }
                    }
                }
            }

        }
    }
}
