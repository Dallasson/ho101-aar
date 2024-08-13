package com.app.lockcompose


import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppLockAccessibility"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
        }
        this.serviceInfo = info
        Log.d(TAG, "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val myPackageName = applicationContext.packageName
        val packageName = event.packageName?.toString()
        Log.d(TAG, "Event received: $packageName")

        if (packageName == null || packageName == myPackageName) {
            return
        }

        val appLockManager = AppLockManager(this)
        val lockedPackages = appLockManager.getSelectedPackages()

        if (packageName in lockedPackages) {
            Log.d(TAG, "Locking app: $packageName")
            showLockScreen(packageName)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    private fun showLockScreen(packageName: String) {
        val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
            putExtra("PACKAGE_NAME", packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        Log.d(TAG, "Launching LockScreenActivity for $packageName")
        startActivity(lockIntent)
    }

}
