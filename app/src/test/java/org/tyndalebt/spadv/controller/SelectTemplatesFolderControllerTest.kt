package org.tyndalebt.spadv.controller

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
import org.tyndalebt.spadv.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER
import org.tyndalebt.spadv.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO
import org.tyndalebt.spadv.controller.SelectTemplatesFolderController.Companion.UPDATE_TEMPLATES_FOLDER
import org.tyndalebt.spadv.model.Workspace
import org.tyndalebt.spadv.view.BaseActivityView

@RunWith(MockitoJUnitRunner::class)
class SelectTemplatesFolderControllerTest {

    @Mock lateinit var view: BaseActivityView
    @Mock lateinit var workspace: Workspace
    @Mock lateinit var context: Context
    @Mock lateinit var uri: Uri

    @InjectMocks lateinit var controller: SelectTemplatesFolderController

    // The demo template should be added to the templates folder only when the
    // request is SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO
    @Test
    fun testShouldAddDemoToWorkspace() {
        assertTrue(controller.shouldAddDemoToWorkspace(SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO))
        assertFalse(controller.shouldAddDemoToWorkspace(SELECT_TEMPLATES_FOLDER))
        assertFalse(controller.shouldAddDemoToWorkspace(UPDATE_TEMPLATES_FOLDER))
        assertFalse(controller.shouldAddDemoToWorkspace(-1))
    }

    // When a new templates folder is selected:
    // 1. setup the new workspace path
    // 2. request permission from OS for the folder
    @Test
    fun testSetupWorkspace_SELECT_TEMPLATES_FOLDER() {
        controller.setupWorkspace(SELECT_TEMPLATES_FOLDER, uri)

        verify(workspace, times(1)).setupWorkspacePath(context, uri)
        verify(view, times(1)).takePersistableUriPermission(uri)
    }

}