package com.app.lockcompose

import ShowAppList
import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.lockcompose.screens.WelcomeScreen
import com.app.lockcompose.ui.theme.LockComposeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LockComposeTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") { WelcomeScreen(navController) }
                    composable("main") { MainScreen(navController) }
                    composable("showAppList") { ShowAppList() }
                }
            }
        }
    }

    @Composable
    fun MainScreen(navController: NavController) {
        val context = LocalContext.current
        val hasUsageStatsPermission = remember { mutableStateOf(hasUsageStatsPermission(context)) }
        val hasOverlayPermission = remember { mutableStateOf(hasOverlayPermission(context)) }
        val hasNotificationPermission = remember {
            mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                isNotificationPermissionGranted(context)
            } else {
                true
            })
        }
        val isAccessibilityServiceEnabled = remember { mutableStateOf(isAccessibilityServiceEnabled(context, AppLockAccessibilityService::class.java)) }

        val intent = Intent(this, AppLockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        fun updatePermissionStatus() {
            hasUsageStatsPermission.value = hasUsageStatsPermission(context)
            hasOverlayPermission.value = hasOverlayPermission(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasNotificationPermission.value = isNotificationPermissionGranted(context)
            }
            isAccessibilityServiceEnabled.value = isAccessibilityServiceEnabled(context, AppLockAccessibilityService::class.java)
        }

        val requestOverlayPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updatePermissionStatus()
        }
        val requestUsageStatsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updatePermissionStatus()
        }
        val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updatePermissionStatus()
        }
        val requestAccessibilityPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updatePermissionStatus()
        }

        fun hasAllPermissions(): Boolean {
            return hasUsageStatsPermission.value && hasOverlayPermission.value && hasNotificationPermission.value && isAccessibilityServiceEnabled.value
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Display permission rows
            PermissionRow(
                label = stringResource(id = R.string.label_overlay_permission),
                isGranted = hasOverlayPermission.value,
                onClick = {
                    val overLayIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    } else {
                        TODO("VERSION.SDK_INT < M")
                    }
                    requestOverlayPermissionLauncher.launch(overLayIntent)
                }
            )
            PermissionRow(
                label = stringResource(id = R.string.label_usage_access_permission),
                isGranted = hasUsageStatsPermission.value,
                onClick = {
                    val usageAccessIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    requestUsageStatsPermissionLauncher.launch(usageAccessIntent)
                }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionRow(
                    label = stringResource(id = R.string.label_notification_permission),
                    isGranted = hasNotificationPermission.value,
                    onClick = {
                        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        i.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        requestNotificationPermissionLauncher.launch(i)
                    }
                )
            }
            PermissionRow(
                label = stringResource(id = R.string.label_accessibility_permission),
                isGranted = isAccessibilityServiceEnabled.value,
                onClick = {
                    val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    requestAccessibilityPermissionLauncher.launch(i)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Image at the bottom center of the screen
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .clickable {
                        navController.navigate("main")
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.arrow),
                    contentDescription = stringResource(id = R.string.proceed_to_app_list),
                    modifier = Modifier
                        .clickable {
                            if (hasAllPermissions()) {
                                navController.navigate("showAppList")
                            } else {
                                Toast.makeText(context, "Please grant all permissions", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .size(48.dp) // Adjust size as needed
                )
            }
        }
    }

    @Composable
    fun PermissionRow(label: String, isGranted: Boolean, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(
                    id = if (isGranted) R.drawable.baseline_check_24 else R.drawable.uncheck
                ),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }
    private fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            TODO("VERSION.SDK_INT < M")
        }
    }
    private fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false // Handle null value by returning false

        val colonSplitter = TextUtils.SimpleStringSplitter(':').apply {
            setString(enabledServices)
        }

        val componentNameString = ComponentName(context, service).flattenToString()
        return colonSplitter.iterator().asSequence().any { it.equals(componentNameString, ignoreCase = true) }
    }

}
