package me.ycdev.android.lib.common.perms

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager

object PermissionUtils {
    fun hasPermissions(context: Context, vararg permissions: String): Boolean =
        permissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    fun verifyPermissions(grantResults: IntArray): Boolean =
        grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

    fun requestPermissions(activity: Activity, params: PermissionRequestParams) {
        activity.requestPermissions(params.permissions, params.requestCode)
    }
}
