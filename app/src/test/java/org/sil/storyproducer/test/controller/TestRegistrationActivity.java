package org.sil.storyproducer.test.controller;

import android.content.Intent;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.sil.storyproducer.controller.RegistrationActivity;
import org.sil.storyproducer.model.Workspace;

// disable tests in this file. need to consider if they are still of value. expected activity has changed
//@RunWith(RobolectricTestRunner.class)
public class TestRegistrationActivity {
//    @Test
    public void OnCreate_When_WorkspaceDirectoryNotSet_Should_StartFileTreeActivity() {
//        Workspace.INSTANCE.clearWorkspace();
//        RegistrationActivity registrationActivity = Robolectric.buildActivity(RegistrationActivity.class).create().get();
//
//        Intent startedActivity = Shadows.shadowOf(registrationActivity).peekNextStartedActivity();
//        Assert.assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, startedActivity.getAction());
    }

//    @Test
    public void OnCreate_When_WorkspaceDirectoryIsAlreadySet_Should_NotStartFileTreeActivity() {
//        DocumentFile mockFile = Mockito.mock(DocumentFile.class);
//        Mockito.when(mockFile.exists()).thenReturn(true);
//        Mockito.when(mockFile.getUri()).thenReturn(Uri.parse("mock"));
//        Workspace.INSTANCE.setWorkdocfile(mockFile);
//
//        RegistrationActivity registrationActivity = Robolectric.buildActivity(RegistrationActivity.class).create().get();
//
//        Intent startedActivity = Shadows.shadowOf(registrationActivity).peekNextStartedActivity();
//        Assert.assertNull(startedActivity);
    }
}
