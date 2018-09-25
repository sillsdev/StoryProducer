package org.sil.storyproducer.test.controller;

import android.content.Intent;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;
import org.sil.storyproducer.controller.RegistrationActivity;
import org.sil.storyproducer.controller.SplashScreenActivity;

@RunWith(RobolectricTestRunner.class)
public class TestSplashScreenActivity {
    @Test
    public void OnCreate_When_RegistrationIsIncomplete_Should_StartRegistrationActivity() {
        SplashScreenActivity splashScreenActivity = Robolectric.buildActivity(SplashScreenActivity.class).create().get();

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Intent startedActivity = Shadows.shadowOf(splashScreenActivity).peekNextStartedActivityForResult().intent;
        Assert.assertEquals(RegistrationActivity.class.getName(), startedActivity.getComponent().getClassName());
    }
}
