package com.punchreminder.app.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.punchreminder.app.ui.components.PunchCard
import com.punchreminder.app.ui.components.ScreenHeader
import com.punchreminder.app.ui.theme.LocalPunchAccentColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState

private const val STATE_COMPLETED = "COMPLETED"
private const val STATE_PAUSED = "PAUSED"
private const val STATE_SIGN_OUT_DUE = "SIGN-OUT DUE"
private const val STATE_INCOMPLETE = "INCOMPLETE"
private const val STATE_NOT_SIGNED_OUT = "NOT SIGNED OUT"

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    viewModel: HistoryViewModel = viewModel()
) {
    val historyItems by viewModel.items.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<HistoryItem?>(null) }
    // Only one row may be swiped open at a time.
    var openRowKey by remember { mutableStateOf<String?>(null) }

    // Confirmation dialog for deleting a single record
    if (deleteTarget != null) {
        val target = deleteTarget
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete this record?") },
            text = {
                Text(
                    "This will permanently delete the attendance record for " +
                        "${target?.date}. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    target?.let { viewModel.deleteItem(it.workDate) }
                    deleteTarget = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(
                title = "History",
                subtitle = "Your locally saved attendance confirmations"
            )
        }

        item { InfoBanner() }

        if (historyItems.isEmpty()) {
            item { EmptyState() }
        } else {
            items(items = historyItems, key = { it.workDate }) { item ->
                SwipeableHistoryRow(
                    item = item,
                    isOpen = openRowKey == item.workDate,
                    onOpen = { openRowKey = item.workDate },
                    onClose = { if (openRowKey == item.workDate) openRowKey = null },
                    onDelete = { deleteTarget = item }
                )
            }
        }

        item {
            OutlinedButton(
                onClick = { showClearDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Clear local history",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear local history?") },
            text = {
                Text("This permanently deletes all locally recorded attendance history and cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHistory(); showClearDialog = false }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InfoBanner() {
    val accents = LocalPunchAccentColors.current
    PunchCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accents.statusIconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = accents.onStatusIcon,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = "These are saved on this device, not official greytHR records.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Width of the revealed delete action. */
private val DeleteActionWidth = 96.dp

/** Fling speed (px/s) past which the row snaps towards the fling direction. */
private const val FLING_VELOCITY_THRESHOLD_PX = 700f

/**
 * A swipeable history card. Swipe left to reveal the delete button; tap the button to request
 * deletion. The card snaps open past the halfway point (or on a left fling) and snaps closed
 * otherwise. Only one row is open at a time, coordinated by the parent's [isOpen].
 */
@Composable
private fun SwipeableHistoryRow(
    item: HistoryItem,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val accents = LocalPunchAccentColors.current
    val actionWidthPx = with(LocalDensity.current) { DeleteActionWidth.toPx() }

    // Snap whenever the open state changes (including when another row opens).
    LaunchedEffect(isOpen) {
        offsetX.animateTo(
            targetValue = if (isOpen) -actionWidthPx else 0f,
            animationSpec = tween(durationMillis = 200)
        )
    }

    fun settle(shouldOpen: Boolean) {
        when {
            shouldOpen && !isOpen -> onOpen()
            !shouldOpen && isOpen -> onClose()
            else -> scope.launch {
                offsetX.animateTo(
                    targetValue = if (shouldOpen) -actionWidthPx else 0f,
                    animationSpec = tween(durationMillis = 200)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
    ) {
        // Delete action revealed behind the card when swiped left.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.large)
                .background(accents.dangerContainer),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(DeleteActionWidth)
                    .clickable {
                        onClose()
                        onDelete()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = accents.onDangerContainer,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelMedium,
                    color = accents.onDangerContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Foreground card, draggable horizontally.
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offsetX.snapTo(
                                (offsetX.value + delta).coerceIn(-actionWidthPx, 0f)
                            )
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        val shouldOpen = when {
                            velocity < -FLING_VELOCITY_THRESHOLD_PX -> true
                            velocity > FLING_VELOCITY_THRESHOLD_PX -> false
                            else -> offsetX.value < -actionWidthPx / 2f
                        }
                        settle(shouldOpen)
                    }
                )
                .then(
                    if (isOpen) {
                        // Tapping an open card closes it.
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onClose() }
                    } else Modifier
                )
        ) {
            HistoryRow(item = item)
        }
    }
}

@Composable
private fun EmptyState() {
    PunchCard {
        Text(
            text = "No attendance history yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryRow(item: HistoryItem) {
    PunchCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!item.note.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            StatusPill(item.state)
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            TimeColumn(label = "Sign-in", value = item.signIn, modifier = Modifier.weight(1f))
            TimeColumn(label = "Sign-out", value = item.signOut, modifier = Modifier.weight(1f))
            TimeColumn(label = "Total duration", value = item.totalDuration, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TimeColumn(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value ?: "—",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusPill(state: String) {
    val accents = LocalPunchAccentColors.current
    val label = state.uppercase()
    val container: Color
    val content: Color
    when (label) {
        STATE_COMPLETED -> {
            container = accents.successContainer
            content = accents.onSuccessContainer
        }
        STATE_PAUSED, STATE_SIGN_OUT_DUE, STATE_INCOMPLETE, STATE_NOT_SIGNED_OUT -> {
            container = accents.warningContainer
            content = accents.onWarningContainer
        }
        else -> {
            container = MaterialTheme.colorScheme.surface
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
