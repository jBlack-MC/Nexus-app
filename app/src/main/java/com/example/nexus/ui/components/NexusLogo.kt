package com.example.nexus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.R
import com.example.nexus.ui.theme.BrandGreen
import com.example.nexus.ui.theme.BrandIconBg
import com.example.nexus.ui.theme.BrandPurple

@Composable
fun NexusLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    textSize: Int = 28,
    showText: Boolean = true,
    isDark: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(iconSize * 0.25f))
                .background(if (isDark) BrandIconBg else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Nexus Logo",
                tint = Color.Unspecified,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (showText) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Nexus",
                style = TextStyle(
                    brush = Brush.linearGradient(listOf(BrandPurple, BrandGreen)),
                    fontSize = textSize.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            )
        }
    }
}
