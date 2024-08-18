package com.app.lockcompose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.app.lockcompose.R

@Composable
fun WelcomeScreen(navController: NavController) {
    val context = LocalContext.current
    var isAdminEnabled by remember { mutableStateOf(false) }
    var showPermissionRequest by remember { mutableStateOf(false) }




    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Welcome to the App", style = MaterialTheme.typography.labelLarge)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .clickable {
                    navController.navigate("main")
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.arrow),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .alpha(if (isAdminEnabled) 1f else 0.5f) // Adjust opacity based on enabled state
            )
        }
    }
}