package org.sil.storyproducer.androidtest.utilities

import androidx.test.rule.GrantPermissionRule

object PermissionsGranter {
    fun grantStoryProducerPermissions(): GrantPermissionRule {
        return GrantPermissionRule.grant(
                "android.permission.RECORD_AUDIO",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE")
    }
}