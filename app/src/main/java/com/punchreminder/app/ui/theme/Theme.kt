package com.punchreminder.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** Theme-aware semantic accents, provided by [PunchReminderTheme]. */
val LocalPunchAccentColors = staticCompositionLocalOf { DarkPunchAccentColors }

/** Softly rounded corners, as used by every card and input in the design. */
val PunchReminderShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Theme wrapper. Both a dark and a light scheme are supported; the user chooses between them in
 * Settings, and the choice is persisted. [isSystemInDarkTheme] is only the fallback when no
 * explicit choice is supplied.
 */
@Composable
fun PunchReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPunchAccentColors provides if (darkTheme) DarkPunchAccentColors else LightPunchAccentColors
    ) {
        MaterialTheme(
            colorScheme = colorSchemeFor(darkTheme),
            typography = PunchReminderTypography,
            shapes = PunchReminderShapes,
            content = content
        )
    }
}
