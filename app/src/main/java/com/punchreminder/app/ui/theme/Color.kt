package com.punchreminder.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/* ---------------------------------------------------------------------------
 * Shared palette used by the dark UI.
 *
 * These are exposed as top-level values so screens can reference the exact
 * accent colours used in the design (status card, completed pill, section
 * icons) without abusing MaterialTheme roles that do not carry the right
 * semantic meaning.
 * ------------------------------------------------------------------------- */

/** App window background: near-black with a faint blue cast. */
val AppBackground = Color(0xFF0A0B0E)

/** Card surface used for every bordered panel. */
val CardBackground = Color(0xFF15181D)

/** Hairline border drawn around cards, fields and dividers. */
val CardBorder = Color(0xFF262B33)

val TextPrimary = Color(0xFFF3F6FA)
val TextSecondary = Color(0xFF9BA5B4)

/** Light accent blue: icons, link-style labels and button outlines. */
val AccentBlue = Color(0xFF8AB4F8)

/** Saturated blue used for enabled switches and selected day chips. */
val AccentBlueBright = Color(0xFF3B82F6)

/** Container of the "today's session" status card. */
val StatusCardBlue = Color(0xFF1C3C5E)

/** Bright blue circle that holds the status card icon. */
val StatusIconBlue = Color(0xFF5B9CF5)

val SuccessGreen = Color(0xFF4ADE80)
val SuccessGreenContainer = Color(0xFF14351F)

val WarnAmber = Color(0xFFFBBF24)
val WarnAmberContainer = Color(0xFF3A2E12)

val DangerRed = Color(0xFFFF7B7B)

/**
 * Semantic accent colours that sit outside the Material roles and therefore need explicit
 * light/dark variants. Screens read these through [LocalPunchAccentColors] so the same
 * composable renders correctly in either theme.
 */
@Immutable
data class PunchAccentColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val statusCard: Color,
    val statusIconBackground: Color,
    val onStatusIcon: Color,
    val chipUnselected: Color,
    val onChipUnselected: Color
)

/** Accents tuned for the dark scheme. */
val DarkPunchAccentColors = PunchAccentColors(
    success = SuccessGreen,
    onSuccess = Color(0xFF06210F),
    successContainer = SuccessGreenContainer,
    onSuccessContainer = SuccessGreen,
    warning = WarnAmber,
    warningContainer = WarnAmberContainer,
    onWarningContainer = WarnAmber,
    danger = DangerRed,
    onDanger = Color(0xFF3A0A0A),
    dangerContainer = Color(0xFF4A1414),
    onDangerContainer = Color(0xFFFFDAD6),
    statusCard = StatusCardBlue,
    statusIconBackground = StatusIconBlue,
    onStatusIcon = StatusCardBlue,
    chipUnselected = Color(0xFF2B313A),
    onChipUnselected = TextSecondary
)

/** Accents tuned for the light scheme (darker content, soft containers). */
val LightPunchAccentColors = PunchAccentColors(
    success = Color(0xFF1B7A43),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD7F5E3),
    onSuccessContainer = Color(0xFF0A4524),
    warning = Color(0xFF8A5A00),
    warningContainer = Color(0xFFFFEBC2),
    onWarningContainer = Color(0xFF573900),
    danger = Color(0xFFB3261E),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFF9DEDC),
    onDangerContainer = Color(0xFF410002),
    statusCard = Color(0xFFD6E7FA),
    statusIconBackground = Color(0xFF1B5E9E),
    onStatusIcon = Color(0xFFFFFFFF),
    chipUnselected = Color(0xFFE7ECF3),
    onChipUnselected = Color(0xFF3B4451)
)

/* ------------------------------- Dark scheme ------------------------------ */

private val primaryDark = AccentBlue
private val onPrimaryDark = Color(0xFF04223F)
private val primaryContainerDark = StatusCardBlue
private val onPrimaryContainerDark = TextPrimary
private val secondaryDark = AccentBlueBright
private val onSecondaryDark = Color(0xFF04182E)
private val secondaryContainerDark = StatusCardBlue
private val onSecondaryContainerDark = TextPrimary
private val tertiaryDark = AccentBlue
private val onTertiaryDark = onPrimaryDark
private val tertiaryContainerDark = StatusCardBlue
private val onTertiaryContainerDark = TextPrimary
private val errorDark = DangerRed
private val onErrorDark = Color(0xFF3A0A0A)
private val errorContainerDark = Color(0xFF4A1414)
private val onErrorContainerDark = Color(0xFFFFDAD6)

/** Dark color scheme: the primary (and only) look of the app. */
val darkScheme: ColorScheme = ColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = CardBorder,
    surfaceTint = Color.Transparent,
    inverseSurface = TextPrimary,
    inverseOnSurface = AppBackground,
    inversePrimary = Color(0xFF1B5E9E),
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    scrim = Color(0xFF000000)
)

/* ------------------------------ Light scheme ------------------------------ */

private val backgroundLight = Color(0xFFF5F7FA)
private val surfaceLight = Color(0xFFFFFFFF)
private val onSurfaceLight = Color(0xFF11161D)
private val onSurfaceVariantLight = Color(0xFF56606E)
private val outlineLight = Color(0xFFDCE2EA)

/** Light color scheme, kept for completeness (the app ships dark). */
val lightScheme: ColorScheme = ColorScheme(
    primary = Color(0xFF1B5E9E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E7FA),
    onPrimaryContainer = Color(0xFF06213D),
    secondary = AccentBlueBright,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E7FA),
    onSecondaryContainer = Color(0xFF06213D),
    tertiary = Color(0xFF1B5E9E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E7FA),
    onTertiaryContainer = Color(0xFF06213D),
    background = backgroundLight,
    onBackground = onSurfaceLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineLight,
    surfaceTint = Color.Transparent,
    inverseSurface = onSurfaceLight,
    inverseOnSurface = surfaceLight,
    inversePrimary = Color(0xFF8AB4F8),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF000000)
)

/** Returns the appropriate color scheme based on the requested dark/light mode. */
fun colorSchemeFor(darkTheme: Boolean): ColorScheme =
    if (darkTheme) darkScheme else lightScheme
