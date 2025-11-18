package com.dps.droidpadmacos.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window size class for responsive layouts
 */
enum class WindowSizeClass {
    COMPACT,    // Small phones (< 600dp width)
    MEDIUM,     // Large phones / Small tablets (600dp - 840dp)
    EXPANDED    // Large tablets (> 840dp)
}

/**
 * Orientation
 */
enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE
}

/**
 * Screen size information
 */
data class ScreenSize(
    val widthDp: Dp,
    val heightDp: Dp,
    val widthPx: Int,
    val heightPx: Int,
    val windowSizeClass: WindowSizeClass,
    val orientation: ScreenOrientation
) {
    val isCompact: Boolean get() = windowSizeClass == WindowSizeClass.COMPACT
    val isMedium: Boolean get() = windowSizeClass == WindowSizeClass.MEDIUM
    val isExpanded: Boolean get() = windowSizeClass == WindowSizeClass.EXPANDED
    val isPortrait: Boolean get() = orientation == ScreenOrientation.PORTRAIT
    val isLandscape: Boolean get() = orientation == ScreenOrientation.LANDSCAPE
}

/**
 * Get current screen size information
 */
@Composable
fun rememberScreenSize(): ScreenSize {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp

    val windowSizeClass = when {
        widthDp < 600.dp -> WindowSizeClass.COMPACT
        widthDp < 840.dp -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }

    val orientation = when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> ScreenOrientation.LANDSCAPE
        else -> ScreenOrientation.PORTRAIT
    }

    return ScreenSize(
        widthDp = widthDp,
        heightDp = heightDp,
        widthPx = with(density) { widthDp.toPx().toInt() },
        heightPx = with(density) { heightDp.toPx().toInt() },
        windowSizeClass = windowSizeClass,
        orientation = orientation
    )
}
