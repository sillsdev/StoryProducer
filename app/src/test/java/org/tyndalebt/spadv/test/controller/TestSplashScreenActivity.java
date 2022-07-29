package org.tyndalebt.spadv.test.controller;

import android.content.Intent;

import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;
import org.tyndalebt.spadv.controller.MainActivity;
import org.tyndalebt.spadv.controller.RegistrationActivity;
import org.tyndalebt.spadv.controller.SplashScreenActivity;
import org.tyndalebt.spadv.model.Workspace;

@RunWith(RobolectricTestRunner.class)
public class TestSplashScreenActivity {
    @Test
    public void OnCreate_When_RegistrationIsIncomplete_Should_StartRegistrationActivity() {
        Workspace.INSTANCE.getRegistration().setComplete(false);
        SplashScreenActivity splashScreenActivity = Robolectric.buildActivity(SplashScreenActivity.class).create().get();

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Intent startedActivity = Shadows.shadowOf(splashScreenActivity).peekNextStartedActivityForResult().intent;
        Assert.assertEquals(RegistrationActivity.class.getName(), startedActivity.getComponent().getClassName());
    }

    @Test
    public void OnCreate_When_RegistrationIsComplete_Should_StartMainActivity() {
        Workspace.INSTANCE.getRegistration().setComplete(true);
        SplashScreenActivity splashScreenActivity = Robolectric.buildActivity(SplashScreenActivity.class).create().get();

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Intent startedActivity = Shadows.shadowOf(splashScreenActivity).peekNextStartedActivityForResult().intent;
        Assert.assertEquals(MainActivity.class.getName(), startedActivity.getComponent().getClassName());
    }
}