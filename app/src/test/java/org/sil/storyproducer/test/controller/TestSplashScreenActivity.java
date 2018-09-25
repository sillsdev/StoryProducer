package org.sil.storyproducer.test.controller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;
import org.sil.storyproducer.controller.MainActivity;
import org.sil.storyproducer.controller.RegistrationActivity;
import org.sil.storyproducer.controller.SplashScreenActivity;
import static org.mockito.Mockito.*;
import org.sil.storyproducer.model.Workspace;

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
