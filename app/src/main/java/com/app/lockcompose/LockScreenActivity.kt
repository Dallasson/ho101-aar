package com.app.lockcompose


import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


class LockScreenActivity : AppCompatActivity() {

    private lateinit var appLockManager: AppLockManager
    private lateinit var lockUi : LinearLayout
    private lateinit var askPermissionBtn : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_screen)


        //Configure window flags for the lock screen
        // Configure window attributes for full-screen overlay




        lockUi = findViewById(R.id.lockUi)
        askPermissionBtn = findViewById(R.id.askPermission)
        askPermissionBtn.setOnClickListener {
            if (lockUi.visibility == View.GONE){
                lockUi.visibility = View.VISIBLE
                showPassCodeUi()
            }
        }
        appLockManager = AppLockManager(this)

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

    @SuppressLint("ClickableViewAccessibility")
    private fun showPassCodeUi(){


        val btn0 = findViewById<TextView>(R.id.btn0)
        val btn1 = findViewById<TextView>(R.id.btn1)
        val btn2 = findViewById<TextView>(R.id.btn2)
        val btn3 = findViewById<TextView>(R.id.btn3)
        val btn4 = findViewById<TextView>(R.id.btn4)
        val btn5 = findViewById<TextView>(R.id.btn5)
        val btn6 = findViewById<TextView>(R.id.btn6)
        val btn7 = findViewById<TextView>(R.id.btn7)
        val btn8 = findViewById<TextView>(R.id.btn8)
        val btn9 = findViewById<TextView>(R.id.btn9)
        val tick = findViewById<ImageView>(R.id.tick)
        val edit = findViewById<EditText>(R.id.passCodeEdit)

        val passcodeBuilder = StringBuilder()
        val numberButtons = listOf(btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9)

        tick.setOnClickListener {
            val passcode = passcodeBuilder.toString()
            if (passcode == "1234") {
                edit.text.clear()
                removePackage()
                finishAffinity()
            } else {
                Toast.makeText(this,"Pass code is wrong",Toast.LENGTH_LONG).show()
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

    private fun addRemoveIcon(edit : EditText){
        val greenColor = ContextCompat.getColor(this, R.color.greenColor)
        val colorFilter = PorterDuffColorFilter(greenColor, PorterDuff.Mode.SRC_IN)
        edit.compoundDrawablesRelative[2]?.colorFilter = colorFilter
    }


    private fun removePackage() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            registerReceiver(packageRemovalReceiver, IntentFilter("PACKAGE_REMOVED"), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(packageRemovalReceiver, IntentFilter("PACKAGE_REMOVED"))
        }

        val packageName = intent.getStringExtra("PACKAGE_NAME")
        if (packageName != null) {
            val lockedPackages = appLockManager.getSelectedPackages()
            if (lockedPackages.contains(packageName)) {
                appLockManager.removePackage(packageName)
                appLockManager.updateAccessList(packageName)
                // Send a broadcast when a package is removed
                val intent = Intent("PACKAGE_REMOVED")
                intent.putExtra("PACKAGE_NAME", packageName)
                sendBroadcast(intent)
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(packageRemovalReceiver)
        super.onDestroy()
    }


}

