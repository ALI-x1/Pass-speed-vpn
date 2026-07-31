package io.github.immaghzbad.aetherst.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IosToast(
    message: String?,
    isError: Boolean,
    scaleFactor: Float = 1f
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = (80 * scaleFactor).dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn() + scaleIn(initialScale = 0.85f),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut() + scaleOut(targetScale = 0.85f)
        ) {
            Surface(
                color = Color(0xFF2C2C2E).copy(alpha = 0.95f),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isError) Color(0xFFFF3B30) else Color(0xFF34C759),
                        modifier = Modifier.size((20 * scaleFactor).dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = message ?: "",
                        color = Color.White,
                        fontSize = (14 * scaleFactor).sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
