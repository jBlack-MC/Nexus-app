package com.example.nexus.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nexus.ui.theme.Spacing

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
) {
    // Shimmer-style alpha pulse; held static when the system has animations disabled.
    val reduceMotion = rememberReduceMotion()
    val alpha =
        if (reduceMotion) {
            0.7f
        } else {
            rememberInfiniteTransition(label = "skeleton").animateFloat(
                initialValue = 0.45f,
                targetValue = 0.85f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "skeletonAlpha",
            ).value
        }
    Spacer(
        modifier =
            modifier
                .height(height)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
                    RoundedCornerShape(8.dp),
                ),
    )
}

@Composable
fun ListSkeleton(labelWidth: Float = 0.55f) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.smd),
    ) {
        repeat(5) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.mdCompact),
            ) {
                SkeletonBlock(modifier = Modifier.fillMaxWidth(labelWidth))
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.82f), height = 12.dp)
            }
        }
    }
}

@Composable
fun DetailSkeleton() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.smd),
    ) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.65f), height = 28.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 54.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 54.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.mdCompact)) {
            SkeletonBlock(modifier = Modifier.width(120.dp), height = 42.dp)
            SkeletonBlock(modifier = Modifier.width(120.dp), height = 42.dp)
        }
        repeat(4) { SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 52.dp) }
    }
}

@Composable
fun DashboardSkeleton() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.smd),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
        ) {
            repeat(3) {
                SkeletonBlock(
                    modifier = Modifier.weight(1f),
                    height = 96.dp,
                )
            }
        }
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 120.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 120.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 48.dp)
    }
}
