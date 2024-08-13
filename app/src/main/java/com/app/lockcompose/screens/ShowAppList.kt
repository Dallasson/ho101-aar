
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lockcompose.AppLockManager


@SuppressLint("UnspecifiedRegisterReceiverFlag")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowAppList() {
    val context = LocalContext.current
    val appLockManager = AppLockManager(context)
    val sharedPreferences = context.getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE)

    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val dropdownBackgroundColor = if (isDarkTheme) Color.DarkGray else Color.White
    val cardBackgroundColor = if (isDarkTheme) Color.DarkGray else Color.White

    val allApps = remember { getInstalledApps(context) }

    var selectedApps by remember {
        mutableStateOf(
            allApps.filter {
                it.packageName in (sharedPreferences.getStringSet("selected_package_names", emptySet()) ?: emptySet())
            }.toMutableList()
        )
    }

    var availableApps by remember {
        mutableStateOf(
            allApps.filter { it.packageName !in selectedApps.map { app -> app.packageName } }.toMutableList()
        )
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableStateOf("") }
    val timeIntervals = arrayOf("1 min", "15 min", "30 min", "45 min", "60 min", "75 min", "90 min", "120 min")

    fun saveSelectedPackages() {
        val packageNames = selectedApps.map { it.packageName }.toSet()
        sharedPreferences.edit().putStringSet("selected_package_names", packageNames).apply()
    }

    DisposableEffect(Unit) {
        val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val updatedSelectedApps = allApps.filter {
                    it.packageName in (sharedPreferences.getStringSet("selected_package_names", emptySet()) ?: emptySet())
                }
                selectedApps = updatedSelectedApps.toMutableList()
                availableApps = allApps.filter { it.packageName !in selectedApps.map { app -> app.packageName } }.toMutableList()
            }
        }

        val filter = IntentFilter("UPDATE_APP_LIST")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(updateReceiver, filter)
        }

        onDispose {
            context.unregisterReceiver(updateReceiver)
        }
    }

    Column(
        modifier = Modifier.background(backgroundColor)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .background(color = dropdownBackgroundColor)
        ) {
            OutlinedTextField(
                label = { Text(text = "Select Duration") },
                value = selectedInterval,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                timeIntervals.forEach { interval ->
                    DropdownMenuItem(
                        text = { Text(text = interval) },
                        onClick = {
                            selectedInterval = interval
                            expanded = false
                        }
                    )
                }
            }
        }

        // Selected Apps List
        Text(
            text = "Access List",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 20.dp),
            color = textColor
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
                .heightIn(
                    min = if (selectedApps.isEmpty()) 40.dp else 0.dp,
                    max = 400.dp
                )
        ) {
            items(selectedApps) { app ->
                AppListItem(
                    app = app,
                    onClick = {
                        selectedApps = (selectedApps - app).toMutableList()
                        availableApps = (availableApps + app).toMutableList()
                        saveSelectedPackages()
                    },
                    textColor = textColor,
                    cardBackgroundColor = cardBackgroundColor
                )
            }
        }

        // Available Apps List
        Text(
            text = "Available Apps",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 20.dp),
            color = textColor
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)
        ) {
            items(availableApps) { app ->
                AppListItem(
                    app = app,
                    onClick = {
                        selectedApps = (selectedApps + app).toMutableList()
                        availableApps = (availableApps - app).toMutableList()
                        saveSelectedPackages()
                    },
                    textColor = textColor,
                    cardBackgroundColor = cardBackgroundColor
                )
            }
        }
    }
}

data class InstalledApp(
    val packageName: String,
    val name: String,
    val icon: Drawable?
)

fun getInstalledApps(context: Context): List<InstalledApp> {
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val pkgAppsList: List<ResolveInfo> = context.packageManager.queryIntentActivities(mainIntent, 0)

    return pkgAppsList.map { resolveInfo ->
        val packageName = resolveInfo.activityInfo.packageName
        val name = resolveInfo.loadLabel(context.packageManager).toString()
        val icon = resolveInfo.loadIcon(context.packageManager)
        InstalledApp(packageName, name, icon)
    }
}

fun Drawable.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

@Composable
fun rememberDrawablePainter(drawable: Drawable?): Painter {
    return remember(drawable) {
        if (drawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                BitmapPainter(bitmap.asImageBitmap())
            } else {
                val bitmap = drawable.toBitmap()
                BitmapPainter(bitmap.asImageBitmap())
            }
        } else {
            BitmapPainter(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap())
        }
    }
}

@Composable
fun AppListItem(app: InstalledApp, onClick: () -> Unit, textColor: Color, cardBackgroundColor: Color) {
    val iconPainter = rememberDrawablePainter(app.icon)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor,
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = iconPainter,
                contentDescription = app.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(cardBackgroundColor)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
                    .fillMaxWidth(),
                textAlign = TextAlign.Start,
                color = textColor
            )
        }
    }
}