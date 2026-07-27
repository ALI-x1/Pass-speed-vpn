package io.github.immaghzbad.aetherst.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.immaghzbad.aetherst.model.AetherLogLevel
import io.github.immaghzbad.aetherst.model.LogEntry
import io.github.immaghzbad.aetherst.model.LogLevel
import io.github.immaghzbad.aetherst.ui.AetherViewModel

private val IosCardBg = Color(0xFF1C1C1E)
private val IosSecondaryLabel = Color(0xFF8E8E93)
private val IosActiveBlue = Color(0xFF007AFF)
private val IosActiveGreen = Color(0xFF34C759)
private val IosWarnAmber = Color(0xFFFF9500)
private val IosErrorRed = Color(0xFFFF3B30)
private val IosDebugCyan = Color(0xFF64D2FF)

@Composable
fun LogsScreen(
    viewModel: AetherViewModel,
    bottomContentPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var selectedLevelFilter by remember { mutableStateOf<LogLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, selectedLevelFilter, searchQuery) {
        logs.filter { entry ->
            val levelMatches = selectedLevelFilter == null || entry.level == selectedLevelFilter
            val searchMatches = searchQuery.isEmpty() || entry.message.contains(searchQuery, ignoreCase = true) || entry.tag.contains(searchQuery, ignoreCase = true)
            levelMatches && searchMatches
        }
    }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            try {
                listState.scrollToItem(filteredLogs.size - 1)
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = bottomContentPadding + 16.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AetherST Logs",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Live Aether Core Logs",
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondaryLabel
                )
            }

            Row {
                IconButton(
                    onClick = {
                        viewModel.copyLogs(context)
                        Toast.makeText(context, "Logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Logs",
                        tint = IosActiveBlue
                    )
                }

                IconButton(
                    onClick = { viewModel.clearLogs() },
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Logs",
                        tint = IosErrorRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (config.coreLogLevel == AetherLogLevel.OFF) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = IosActiveBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Core logging is OFF by default to eliminate RAM & background CPU overhead. Set Core Log Level to Info or Debug in Config to record engine events.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryLabel
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search console logs...", color = IosSecondaryLabel, fontSize = 14.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = IosSecondaryLabel) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_logs_field"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = IosCardBg,
                unfocusedContainerColor = IosCardBg,
                focusedBorderColor = IosActiveBlue,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(IosCardBg)
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val filters = listOf(
                "ALL" to null,
                "INFO" to LogLevel.INFO,
                "WARN" to LogLevel.WARN,
                "ERROR" to LogLevel.ERROR,
                "DEBUG" to LogLevel.DEBUG
            )

            filters.forEach { (label, level) ->
                val selected = selectedLevelFilter == level
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) IosActiveBlue else Color.Transparent)
                        .clickable { selectedLevelFilter = level }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else IosSecondaryLabel
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = IosCardBg
        ) {
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val msg = if (config.coreLogLevel == AetherLogLevel.OFF && config.appLogLevel == AetherLogLevel.OFF) {
                        "Logging disabled in Config"
                    } else {
                        "No log records found"
                    }
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryLabel
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    items(
                        items = filteredLogs,
                        key = { it.id }
                    ) { entry ->
                        IosLogLineItem(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
fun IosLogLineItem(entry: LogEntry) {
    val (levelColor, badgeBg) = when (entry.level) {
        LogLevel.INFO -> Pair(IosActiveGreen, IosActiveGreen.copy(alpha = 0.15f))
        LogLevel.WARN -> Pair(IosWarnAmber, IosWarnAmber.copy(alpha = 0.15f))
        LogLevel.ERROR -> Pair(IosErrorRed, IosErrorRed.copy(alpha = 0.15f))
        LogLevel.DEBUG -> Pair(IosDebugCyan, IosDebugCyan.copy(alpha = 0.15f))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141416))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = entry.level.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = levelColor,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF2F2F7),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📅 ${entry.timestamp}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = IosSecondaryLabel,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "•  ${entry.tag}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = IosSecondaryLabel.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}
