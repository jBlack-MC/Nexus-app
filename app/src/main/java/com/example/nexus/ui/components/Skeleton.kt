package com.example.nexus.ui.components

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
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 18.dp
) {
    Spacer(
        modifier = modifier
            .height(height)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                RoundedCornerShape(8.dp)
            )
    )
}

@Composable
fun ListSkeleton(labelWidth: Float = 0.55f) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.65f), height = 28.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 54.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 54.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBlock(modifier = Modifier.width(120.dp), height = 42.dp)
            SkeletonBlock(modifier = Modifier.width(120.dp), height = 42.dp)
        }
        repeat(4) { SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 52.dp) }
    }
}
