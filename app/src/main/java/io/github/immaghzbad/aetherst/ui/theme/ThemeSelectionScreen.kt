package io.github.immaghzbad.aetherst.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ThemeSelectionScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val current = ThemeManager.currentTheme
    val lightThemes = AppThemes.filter { !it.isDark }
    val darkThemes = AppThemes.filter { it.isDark }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Text(
            text = "انتخاب تم",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${AppThemes.size} تم موجود · شبیه تلگرام",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))
        ActiveThemeCard(theme = current)

        Spacer(Modifier.height(28.dp))
        SectionLabel("روشن")
        Spacer(Modifier.height(12.dp))
        ThemeGrid(themes = lightThemes, selectedId = current.id) { id ->
            ThemeManager.setTheme(context, id)
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("تیره")
        Spacer(Modifier.height(12.dp))
        ThemeGrid(themes = darkThemes, selectedId = current.id) { id ->
            ThemeManager.setTheme(context, id)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ActiveThemeCard(theme: AppTheme) {
    val cardBg = if (theme.isDark) theme.colorScheme.surface else theme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(width = 2.dp, color = theme.colorScheme.primary, shape = RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color = Color.Transparent, borderColor = theme.colorScheme.outline)
            Spacer(Modifier.width(6.dp))
            if (!theme.isDark) {
                ColorDot(color = theme.colorScheme.primaryContainer)
                Spacer(Modifier.width(6.dp))
            }
            ColorDot(color = theme.colorScheme.primary, sizeDp = 18.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "${theme.emoji} ${theme.label}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.colorScheme.onSurface
                )
                Text(
                    text = if (theme.isDark) "تم تیره · فعال" else "تم روشن · فعال",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.colorScheme.primary
                )
            }
        }
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(theme.colorScheme.primary)
        )
    }
}

@Composable
private fun ThemeGrid(
    themes: List<AppTheme>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        themes.chunked(3).forEach { rowThemes ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowThemes.forEach { theme ->
                    ThemeMiniCard(
                        theme = theme,
                        selected = theme.id == selectedId,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(theme.id) }
                    )
                }
                repeat(3 - rowThemes.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeMiniCard(
    theme: AppTheme,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (selected) theme.colorScheme.primary else theme.colorScheme.outline.copy(alpha = 0.5f)
    val cardBg = if (theme.isDark) theme.colorScheme.surface else theme.colorScheme.surfaceVariant
    val textColor = theme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 6.dp)
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(theme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = theme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorDot(color = Color.Transparent, borderColor = theme.colorScheme.outline)
                if (!theme.isDark) {
                    ColorDot(color = theme.colorScheme.primaryContainer)
                }
                ColorDot(color = theme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${theme.emoji} ${theme.label}",
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ColorDot(color: Color, borderColor: Color? = null, sizeDp: Dp = 14.dp) {
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, CircleShape) else Modifier
            )
    )
}
