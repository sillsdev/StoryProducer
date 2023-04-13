package org.sil.storyproducer.controller

import android.content.Context
import android.net.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.UPDATE_TEMPLATES_FOLDER
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.view.BaseActivityView

// disable tests in this file. need to consider if they are still of value
// @RunWith(MockitoJUnitRunner::class)
class SelectTemplatesFolderControllerTest {

    @Mock lateinit var view: BaseActivityView
    @Mock lateinit var workspace: Workspace
    @Mock lateinit var context: Context
    @Mock lateinit var uri: Uri

    @InjectMocks lateinit var controller: SelectTemplatesFolderController

    // The demo template should be added to the templates folder only when the
    // request is SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO
    // @Test //Removed this test until Workspace.workdocfile.uri can be mocked
    fun testShouldAddDemoToWorkspace() {
        assertTrue(controller.shouldAddDemoToWorkspace(SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO))
        //now assertFalse?    assertTrue(controller.shouldAddDemoToWorkspace(SELECT_TEMPLATES_FOLDER))
        // now assertFalse?    assertTrue(controller.shouldAddDemoToWorkspace(UPDATE_TEMPLATES_FOLDER))
        assertTrue(controller.shouldAddDemoToWorkspace(-1))
        // TODO: Add some negative tests - such as when there is already a story installed
    }

    // When a new templates folder is selected:
    // 1. setup the new workspace path
    // 2. request permission from OS for the folder
    // @Test // REMOVED TEST UNTIL IT CAN BE FIXED
    // TODO: Put back this test when we can
    fun testSetupWorkspace_SELECT_TEMPLATES_FOLDER() {
        controller.setupWorkspace(SELECT_TEMPLATES_FOLDER, uri)

        verify(workspace, times(1)).setupWorkspacePath(context, uri)
        verify(view, times(1)).takePersistableUriPermission(uri)
    }

}
