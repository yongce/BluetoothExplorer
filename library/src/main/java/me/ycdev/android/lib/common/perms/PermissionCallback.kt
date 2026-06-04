package me.ycdev.android.lib.common.perms

interface PermissionCallback {
    fun onRationaleDenied(requestCode: Int)
}
