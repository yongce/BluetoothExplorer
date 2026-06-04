package me.ycdev.android.lib.common.perms

class PermissionRequestParams {
    var requestCode: Int = 0
    var permissions: Array<String> = emptyArray()
    var rationaleContent: String? = null
    var callback: PermissionCallback? = null
}
