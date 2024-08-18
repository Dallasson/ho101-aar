package com.app.lockcompose



import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppLockAccessibility"
    }

    private lateinit var appLockManager: AppLockManager
    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager

    override fun onServiceConnected() {
        super.onServiceConnected()

        appLockManager = AppLockManager(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        }
        this.serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val myPackageName = applicationContext.packageName

        if (packageName == myPackageName) return

        val lockedPackages = appLockManager.getSelectedPackages()

        if (packageName in lockedPackages) {
            showPartialOverlay(packageName)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    private fun showPartialOverlay(packageName: String) {
        if (overlayView == null) {
            val layoutInflater = LayoutInflater.from(this)
            val overlayLayout = layoutInflater.inflate(R.layout.activity_lock_screen, null)
            val askPermissionBtn = overlayLayout.findViewById<Button>(R.id.askPermission)
            val cancelPermission = overlayLayout.findViewById<Button>(R.id.cancelPermission)
            val lockUi = overlayLayout.findViewById<LinearLayout>(R.id.lockUi)


            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON ,
                PixelFormat.TRANSPARENT
            )

            overlayView = overlayLayout
            windowManager.addView(overlayView, layoutParams)

            askPermissionBtn.setOnClickListener {
                if (lockUi.visibility == View.GONE) {
                    lockUi.visibility = View.VISIBLE
                    askPermissionBtn.visibility = View.GONE
                    cancelPermission.visibility  = View.VISIBLE
                    showPassCodeUi(overlayLayout, packageName)
                }
            }

            cancelPermission.setOnClickListener {
                askPermissionBtn.visibility = View.VISIBLE
                cancelPermission.visibility  = View.GONE
                lockUi.visibility = View.GONE
            }

        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showPassCodeUi(view: View, packageName: String) {
        val btn0 = view.findViewById<TextView>(R.id.btn0)
        val btn1 = view.findViewById<TextView>(R.id.btn1)
        val btn2 = view.findViewById<TextView>(R.id.btn2)
        val btn3 = view.findViewById<TextView>(R.id.btn3)
        val btn4 = view.findViewById<TextView>(R.id.btn4)
        val btn5 = view.findViewById<TextView>(R.id.btn5)
        val btn6 = view.findViewById<TextView>(R.id.btn6)
        val btn7 = view.findViewById<TextView>(R.id.btn7)
        val btn8 = view.findViewById<TextView>(R.id.btn8)
        val btn9 = view.findViewById<TextView>(R.id.btn9)
        val tick = view.findViewById<ImageView>(R.id.tick)

        val edit = view.findViewById<EditText>(R.id.passCodeEdit)

        val passcodeBuilder = StringBuilder()
        val numberButtons = listOf(btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9)

        tick.setOnClickListener {
            val passcode = passcodeBuilder.toString()
            if (passcode == "1234") {
                edit.text.clear()
                removeOverlay()
                removePackage(packageName)
                Toast.makeText(this, "Unlocked successfully", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Passcode is incorrect", Toast.LENGTH_LONG).show()
            }
        }

        numberButtons.forEach { button ->
            button.setOnClickListener {
                passcodeBuilder.append(button.text)
                edit.setText(passcodeBuilder.toString())
            }
        }

        addRemoveIcon(edit)
        edit.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = edit.compoundDrawablesRelative[2]
                if (drawableEnd != null && event.rawX >= edit.right - drawableEnd.bounds.width()) {
                    if (passcodeBuilder.isNotEmpty()) {
                        passcodeBuilder.deleteCharAt(passcodeBuilder.length - 1)
                        edit.setText(passcodeBuilder.toString())
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }



    }

    private fun addRemoveIcon(edit: EditText) {

        val drawableEnd = edit.compoundDrawablesRelative[2]
        if (drawableEnd != null) {
            val greenColor = ContextCompat.getColor(this, R.color.greenColor)
            val colorFilter = PorterDuffColorFilter(greenColor, PorterDuff.Mode.SRC_IN)
            drawableEnd.colorFilter = colorFilter
            edit.invalidate()
        }
    }
    private val packageRemovalReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "PACKAGE_REMOVED") {
                val packageName = intent.getStringExtra("PACKAGE_NAME")
                packageName?.let {
                    appLockManager.removePackageFromAccessList(it)
                    // Send an update broadcast
                    val updateIntent = Intent("UPDATE_APP_LIST")
                    sendBroadcast(updateIntent)
                }
            }
        }
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun removePackage(packageName: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageRemovalReceiver, IntentFilter("PACKAGE_REMOVED"), RECEIVER_EXPORTED)
        } else {
            registerReceiver(packageRemovalReceiver, IntentFilter("PACKAGE_REMOVED"))
        }

        val lockedPackages = appLockManager.getSelectedPackages()
        if (lockedPackages.contains(packageName)) {
            appLockManager.removePackage(packageName)
            appLockManager.updateAccessList(packageName)
            val intent = Intent("PACKAGE_REMOVED")
            intent.putExtra("PACKAGE_NAME", packageName)
            sendBroadcast(intent)
        }
    }
}