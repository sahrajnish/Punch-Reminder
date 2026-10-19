package com.punchreminder.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.punchreminder.app.domain.AttendanceState
import com.punchreminder.app.ui.components.HDivider
import com.punchreminder.app.ui.components.PunchCard
import com.punchreminder.app.ui.components.ScreenHeader
import com.punchreminder.app.ui.components.SectionLabel
import com.punchreminder.app.ui.components.VDivider
import com.punchreminder.app.ui.theme.LocalPunchAccentColors

import androidx.compose.foundation.ScrollState

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    pauseRequested: Boolean = false,
    onPauseRequestConsumed: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accents = LocalPunchAccentColors.current

    var showPauseDialog by remember { mutableStateOf(false) }
    var selectedPauseReason by remember { mutableStateOf(PauseReasons.first()) }

    LaunchedEffect(pauseRequested) {
        if (pauseRequested) {
            showPauseDialog = true
            onPauseRequestConsumed()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "Punch Reminder",
            subtitle = "greytHR attendance reminders"
        )

        DateTimeCard(uiState)

        StatusCard(uiState)

        SessionCard(uiState)

        when {
            uiState.canSignIn -> PrimaryActionButton(
                text = "I\u2019ve signed in",
                icon = Icons.AutoMirrored.Filled.Login,
                containerColor = accents.success,
                contentColor = accents.onSuccess,
                onClick = { viewModel.confirmSignIn() }
            )
            uiState.canSignOut -> PrimaryActionButton(
                text = "I\u2019ve signed out",
                icon = Icons.AutoMirrored.Filled.Logout,
                containerColor = accents.danger,
                contentColor = accents.onDanger,
                onClick = { viewModel.confirmSignOut() }
            )
            uiState.state == AttendanceState.NOT_SIGNED_IN && uiState.signInTimeText.isNotBlank() -> {
                Text(
                    text = "Sign-in action available after ${uiState.signInTimeText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        when {
            uiState.state == AttendanceState.NOT_SIGNED_IN -> {
                TextButton(
                    onClick = { showPauseDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Pause reminders for today",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            uiState.isSkipped -> {
                TextButton(
                    onClick = { viewModel.resumeToday() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Resume reminders",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { viewModel.openGreytHR() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Open greytHR",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showPauseDialog) {
        PauseTodayDialog(
            selectedReason = selectedPauseReason,
            onSelectReason = { selectedPauseReason = it },
            onConfirm = {
                viewModel.pauseToday(selectedPauseReason)
                showPauseDialog = false
            },
            onDismiss = { showPauseDialog = false }
        )
    }
}

@Composable
private fun DateTimeCard(uiState: HomeUiState) {
    PunchCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(uiState.currentDate.uppercase())
        Spacer(Modifier.height(8.dp))
        Text(
            text = uiState.currentTime,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusCard(uiState: HomeUiState) {
    val accents = LocalPunchAccentColors.current
    PunchCard(modifier = Modifier.fillMaxWidth(), containerColor = accents.statusCard) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accents.statusIconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon(uiState),
                    contentDescription = null,
                    tint = accents.onStatusIcon,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.statusTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = uiState.statusSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SessionCard(uiState: HomeUiState) {
    val accents = LocalPunchAccentColors.current
    PunchCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            TimeBlock(
                icon = Icons.AutoMirrored.Filled.Login,
                tint = accents.success,
                label = "SIGNED IN",
                value = uiState.actualSignIn ?: "—",
                modifier = Modifier.weight(1f)
            )
            VDivider(Modifier.padding(horizontal = 16.dp))
            TimeBlock(
                icon = Icons.AutoMirrored.Filled.Logout,
                tint = accents.danger,
                label = "SIGNED OUT",
                value = uiState.actualSignOut ?: "—",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))
        HDivider()
        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.NotificationsNone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "Next reminder",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = uiState.nextReminderText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun TimeBlock(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            SectionLabel(label)
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun statusIcon(uiState: HomeUiState): ImageVector = when (uiState.state) {
    AttendanceState.SIGNED_OUT -> Icons.Filled.Check
    AttendanceState.SIGNED_IN -> Icons.AutoMirrored.Filled.Login
    else -> Icons.Filled.NotificationsNone
}

/** Reasons offered when pausing a day's reminders. */
private val PauseReasons = listOf("PTO", "Company Holiday", "Sick Leave", "Other")

@Composable
private fun PauseTodayDialog(
    selectedReason: String,
    onSelectReason: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Pause reminders for today?") },
        text = {
            Column {
                Text(
                    text = "No reminders will fire until tomorrow. This resets automatically at midnight.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                PauseReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onSelectReason(reason) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { onSelectReason(reason) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Pause today")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
