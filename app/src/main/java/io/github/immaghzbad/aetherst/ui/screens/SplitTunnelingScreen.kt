package io.github.immaghzbad.aetherst.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import io.github.immaghzbad.aetherst.model.AppInfo

private val IosCardBg = Color(0xFF1C1C1E)
private val IosSecondaryLabel = Color(0xFF8E8E93)
private val IosActiveBlue = Color(0xFF007AFF)

@Composable
fun SplitTunnelingScreen(
    apps: List<AppInfo>,
    excludedPackages: Set<String>,
    onToggleApp: (String) -> Unit,
    onBack: () -> Unit,
    scaleFactor: Float = 1f
) {
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onBack)

    val filteredApps = remember(apps, searchQuery, selectedTab, excludedPackages) {
        apps.filter { app ->
            val matchesTab = if (selectedTab == 0) !app.isSystemApp else app.isSystemApp
            val matchesSearch = searchQuery.isEmpty() || 
                               app.name.contains(searchQuery, ignoreCase = true) || 
                               app.packageName.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesSearch
        }.sortedWith(
            compareByDescending<AppInfo> { excludedPackages.contains(it.packageName) }
                .thenBy { it.name.lowercase() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = (16 * scaleFactor).dp, vertical = (12 * scaleFactor).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size((40 * scaleFactor).dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size((24 * scaleFactor).dp))
            }
            Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
            Text(
                text = "Split Tunneling",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = (22 * scaleFactor).sp
            )
        }

        Box(modifier = Modifier.padding(horizontal = (16 * scaleFactor).dp, vertical = (8 * scaleFactor).dp)) {
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height((44 * scaleFactor).dp)
                    .background(IosCardBg, RoundedCornerShape(12.dp)),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = (14 * scaleFactor).sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                singleLine = true,
                cursorBrush = SolidColor(IosActiveBlue),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.padding(horizontal = (12 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = IosSecondaryLabel, modifier = Modifier.size((20 * scaleFactor).dp))
                        Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text("Search applications...", color = IosSecondaryLabel, fontSize = (13 * scaleFactor).sp)
                            }
                            innerTextField()
                        }
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = (16 * scaleFactor).dp, vertical = (12 * scaleFactor).dp)
                .background(IosCardBg, RoundedCornerShape(10.dp))
                .padding(2.dp)
        ) {
            listOf("User Apps", "System Apps").forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) IosActiveBlue else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = (8 * scaleFactor).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else IosSecondaryLabel,
                        fontSize = (12 * scaleFactor).sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                AppLineItem(
                    app = app,
                    isExcluded = excludedPackages.contains(app.packageName),
                    onToggle = { onToggleApp(app.packageName) },
                    scaleFactor = scaleFactor
                )
            }
        }
    }
}

@Composable
private fun AppLineItem(
    app: AppInfo,
    isExcluded: Boolean,
    onToggle: () -> Unit,
    scaleFactor: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = (16 * scaleFactor).dp, vertical = (10 * scaleFactor).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = remember(app.packageName) { app.icon?.toBitmap()?.asImageBitmap() }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .size((40 * scaleFactor).dp)
                    .clip(RoundedCornerShape((8 * scaleFactor).dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size((40 * scaleFactor).dp)
                    .background(IosCardBg, RoundedCornerShape((8 * scaleFactor).dp))
            )
        }
        
        Spacer(modifier = Modifier.width((14 * scaleFactor).dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = (15 * scaleFactor).sp,
                maxLines = 1
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = IosSecondaryLabel,
                fontSize = (10 * scaleFactor).sp,
                maxLines = 1
            )
        }

        Checkbox(
            checked = isExcluded,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = IosActiveBlue,
                uncheckedColor = IosSecondaryLabel,
                checkmarkColor = Color.White
            ),
            modifier = Modifier.scale(scaleFactor)
        )
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
)
