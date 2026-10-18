package com.punchreminder.app.ui

import android.Manifest
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punchreminder.app.DiContainer
import com.punchreminder.app.ui.components.HDivider
import com.punchreminder.app.ui.home.HomeScreen
import com.punchreminder.app.ui.history.HistoryScreen
import com.punchreminder.app.ui.settings.SettingsScreen
import com.punchreminder.app.ui.theme.PunchReminderTheme
import com.punchreminder.app.ui.theme.colorSchemeFor

/** The three destinations reachable from the bottom navigation bar. */
private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    HISTORY("History", Icons.AutoMirrored.Filled.Assignment),
    SETTINGS("Settings", Icons.Filled.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply the persisted theme before the first frame so a light-theme user does not see a
        // dark window flash while Compose starts up.
        applyThemeChrome(darkTheme = DiContainer.settingsRepository.settings.value.darkTheme)

        setContent {
            val settings by DiContainer.settingsRepository.settings.collectAsState()
            val darkTheme = settings.darkTheme
            LaunchedEffect(darkTheme) { applyThemeChrome(darkTheme) }
            PunchReminderTheme(darkTheme = darkTheme) {
                AppRoot()
            }
        }
    }

    /**
     * Match the window background and system-bar icon colours to the selected app theme.
     * Edge-to-edge lets the Scaffold background show through the transparent system bars.
     */
    private fun applyThemeChrome(darkTheme: Boolean) {
        window.setBackgroundDrawable(
            ColorDrawable(colorSchemeFor(darkTheme).background.toArgb())
        )
        val transparent = android.graphics.Color.TRANSPARENT
        val barStyle = if (darkTheme) {
            SystemBarStyle.dark(transparent)
        } else {
            SystemBarStyle.light(transparent, transparent)
        }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
    }
}

@Composable
fun AppRoot() {
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }

    // Notification permission (Android 13+). Requested once for the whole app.
    var notificationRequested by rememberSaveable { mutableStateOf(false) }
    val requestNotification = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result handling is not required; the system remembers the choice. */ }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationRequested) {
        LaunchedEffect(Unit) {
            notificationRequested = true
            requestNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                AppTab.HOME -> HomeScreen()
                AppTab.HISTORY -> HistoryScreen()
                AppTab.SETTINGS -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun AppBottomBar(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth()) {
            HDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AppTab.entries.forEach { tab ->
                    AppBottomBarItem(
                        tab = tab,
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.AppBottomBarItem(tab: AppTab, selected: Boolean, onClick: () -> Unit) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
