package com.dps.droidpadmacos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dps.droidpadmacos.R

/**
 * Centralized dimension resources that adapt to different screen sizes
 */
object Dimens {

    @Composable
    fun mainHorizontalPadding(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.main_horizontal_padding).toDp() }
    }

    @Composable
    fun mainVerticalPadding(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.main_vertical_padding).toDp() }
    }

    @Composable
    fun statusIconSize(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.status_icon_size).toDp() }
    }

    @Composable
    fun statusIconGlowSize(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.status_icon_glow_size).toDp() }
    }

    @Composable
    fun macbookIconSize(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.macbook_icon_size).toDp() }
    }

    @Composable
    fun statusTextSize(): TextUnit {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.status_text_size).toSp() }
    }

    @Composable
    fun subtitleTextSize(): TextUnit {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.subtitle_text_size).toSp() }
    }

    @Composable
    fun buttonHeight(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.button_height).toDp() }
    }

    @Composable
    fun cardPadding(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.card_padding).toDp() }
    }

    @Composable
    fun spacingSmall(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.spacing_small).toDp() }
    }

    @Composable
    fun spacingMedium(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.spacing_medium).toDp() }
    }

    @Composable
    fun spacingLarge(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.spacing_large).toDp() }
    }

    @Composable
    fun spacingXLarge(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.spacing_xlarge).toDp() }
    }

    @Composable
    fun keyboardKeyWidth(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.keyboard_key_width).toDp() }
    }

    @Composable
    fun keyboardKeyHeight(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.keyboard_key_height).toDp() }
    }

    @Composable
    fun keyboardKeyTextSize(): TextUnit {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.keyboard_key_text_size).toSp() }
    }

    @Composable
    fun keyboardLabelTextSize(): TextUnit {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.keyboard_label_text_size).toSp() }
    }

    @Composable
    fun keyboardPadding(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.keyboard_padding).toDp() }
    }

    @Composable
    fun keyboardSpacing(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.keyboard_spacing).toDp() }
    }

    @Composable
    fun closeButtonSize(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.close_button_size).toDp() }
    }

    @Composable
    fun gestureHintIconSize(): TextUnit {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.gesture_hint_icon_size).toSp() }
    }

    @Composable
    fun gestureHintTextSize(): TextUnit {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.gesture_hint_text_size).toSp() }
    }

    @Composable
    fun gestureGuideIconSize(): TextUnit {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.gesture_guide_icon_size).toSp() }
    }

    @Composable
    fun gestureGuideTextSize(): TextUnit {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.gesture_guide_text_size).toSp() }
    }

    @Composable
    fun connectionBadgePaddingHorizontal(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.connection_badge_padding_horizontal).toDp() }
    }

    @Composable
    fun connectionBadgePaddingVertical(): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        return with(density) { context.resources.getDimension(R.dimen.connection_badge_padding_vertical).toDp() }
    }
}
