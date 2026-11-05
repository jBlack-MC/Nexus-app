package com.example.nexus.ui.components

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Shared motion durations so every screen animates with the same rhythm
 * (all one-shot animations stay within the app's 150–300ms budget).
 */
object Motion {
    /** Forward screen enter (slide + fade). */
    const val SCREEN_ENTER_MS = 220

    /** Screen exit (keeps enter/exit asymmetrical but consistent). */
    const val SCREEN_EXIT_MS = 180

    /** Lazy list item placement/reflow. */
    const val ITEM_MS = 220

    /** Lazy list item fade in/out. */
    const val FADE_MS = 150

    /** Checkbox completion bounce (90ms pop + 140ms settle). */
    const val BOUNCE_POP_MS = 90
    const val BOUNCE_SETTLE_MS = 140

    /** Task-created success checkmark. */
    const val SUCCESS_MS = 260
}

/**
 * True when the system has animations disabled — i.e.
 * `Settings.Global.ANIMATOR_DURATION_SCALE == 0`, surfaced through
 * [ValueAnimator.areAnimatorsEnabled], exactly how SplashIntroScreen detects it. That keeps
 * SplashIntroScreenTest's reduced-motion setup (which flips the same underlying scale, whether
 * via reflection or `settings put global animator_duration_scale 0`) in sync with this helper.
 *
 * Under this flag every non-essential animation in the app is skipped or rendered in its
 * final/static state (nav transitions, list reflow, bounces, shimmer, success checkmark).
 */
@Composable
fun rememberReduceMotion(): Boolean = remember {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()
}
