package io.github.immaghzbad.aetherst.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.immaghzbad.aetherst.model.OnboardingStep
import io.github.immaghzbad.aetherst.ui.AetherViewModel
import io.github.immaghzbad.aetherst.ui.OnboardingViewModel
import kotlin.math.roundToInt

private val IosNavBackground = Color(0xFF1C1C1E)
private val IosNavActiveBlue = Color(0xFF007AFF)
private val IosNavInactiveGrey = Color(0xFF8E8E93)
private val BarContentHeight = 80.dp
private val ButtonSize = 56.dp
private val ButtonCenterY = 22.dp
private val CircleGap = 6.dp
private val BarTopY = 20.dp
private val ItemBottomPadding = 10.dp

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

private fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isIgnoringBatteryOptimizations(packageName) ?: true
}

@Composable
fun MainScreen(viewModel: AetherViewModel) {
    val context = LocalContext.current
    val activity = context.findComponentActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return OnboardingViewModel(context.applicationContext) as T
            }
        },
    )

    val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsStateWithLifecycle()
    val onboardingState by onboardingViewModel.state.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val currentStep by rememberUpdatedState(onboardingState.currentStep)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (currentStep == OnboardingStep.BATTERY_OPTIMIZATION && context.isIgnoringBatteryOptimizations()) {
                    onboardingViewModel.moveToNextStep()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SideEffect {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity?.window?.isNavigationBarContrastEnforced = false
        }
    }

    if (!isOnboardingComplete) {
        val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val intent = VpnService.prepare(context)
            if (intent == null) {
                onboardingViewModel.moveToNextStep()
            }
        }
        val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onboardingViewModel.moveToNextStep()
            } else {
                onboardingViewModel.showNotificationError()
            }
        }

        OnboardingScreen(
            state = onboardingState,
            onGetStarted = { onboardingViewModel.moveToNextStep() },
            onRetryRegistration = { onboardingViewModel.startProtocolTests() },
            onCancelRegistration = { onboardingViewModel.cancelTests() },
            onUpdateScanMode = { onboardingViewModel.updateScanMode(it) },
            onRequestVpnPermission = {
                val intent = VpnService.prepare(context)
                if (intent != null) vpnLauncher.launch(intent) else onboardingViewModel.moveToNextStep()
            },
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onboardingViewModel.moveToNextStep()
                }
            },
            onRequestBatteryOptimization = {
                if (context.isIgnoringBatteryOptimizations()) {
                    onboardingViewModel.moveToNextStep()
                } else {
                    runCatching {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }.onFailure {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                }
            },
            onFinish = onboardingViewModel::moveToNextStep
        )
        return
    }

    if (updateInfo != null) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val scaleFactor = (this.maxWidth.value / 411f).coerceIn(0.7f, 1.1f)
            UpdateScreen(
                info = updateInfo!!,
                onDismiss = { viewModel.dismissUpdate() },
                scaleFactor = scaleFactor
            )
        }
        return
    }

    DashboardContent(viewModel)
}

@Composable
private fun DashboardContent(viewModel: AetherViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSplitTunneling by remember { mutableStateOf(false) }
    
    val config by viewModel.config.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val sessionTraffic by viewModel.sessionTraffic.collectAsStateWithLifecycle()
    val ipInfo by viewModel.ipInfo.collectAsStateWithLifecycle()
    val pingState by viewModel.pingState.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.toggleVpn(context) {}
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission required.", Toast.LENGTH_LONG).show()
        }
    }

    fun handleVpnToggle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.toggleVpn(context) {
            val intent = VpnService.prepare(context)
            if (intent != null) vpnPermissionLauncher.launch(intent)
        }
    }

    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val saveableStateHolder = rememberSaveableStateHolder()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = this.maxWidth
        val scaleFactor = (screenWidth.value / 411f).coerceIn(0.7f, 1.1f)

        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Crossfade(targetState = if (showSplitTunneling) 99 else selectedTab, animationSpec = tween(400), label = "screen_transition") { tab ->
                saveableStateHolder.SaveableStateProvider(tab) {
                    when (tab) {
                        0 -> DashboardScreen(
                            config = config,
                            connectionState = connectionState,
                            elapsedSeconds = elapsedSeconds,
                            sessionTraffic = sessionTraffic,
                            ipInfo = ipInfo,
                            pingState = pingState,
                            onToggleVpn = { handleVpnToggle() },
                            onUpdateProtocol = { proto -> viewModel.updateConfig(config.copy(protocol = proto)) },
                            onRefreshIpInfo = { viewModel.refreshIpInfo() },
                            onRefreshPing = { viewModel.refreshPing() },
                            bottomContentPadding = BarContentHeight + navBarHeight
                        )
                        1 -> SettingsScreen(
                            config = config,
                            onUpdateConfig = { viewModel.updateConfig(it) },
                            onApplyPreset = { preset ->
                                viewModel.applyPreset(preset)
                                Toast.makeText(context, "Applied preset profile!", Toast.LENGTH_SHORT).show()
                            },
                            onOpenSplitTunneling = { showSplitTunneling = true },
                            bottomContentPadding = BarContentHeight + navBarHeight
                        )
                        2 -> LogsScreen(viewModel = viewModel, bottomContentPadding = BarContentHeight + navBarHeight)
                        3 -> AboutUsScreen(bottomContentPadding = BarContentHeight + navBarHeight)
                        99 -> SplitTunnelingScreen(
                            apps = installedApps,
                            excludedPackages = config.excludedPackages,
                            onToggleApp = { viewModel.toggleExcludedPackage(it) },
                            onBack = { showSplitTunneling = false },
                            scaleFactor = scaleFactor
                        )
                    }
                }
            }
        }
        if (!showSplitTunneling) {
            CurvedNavBar(selectedTab = selectedTab, navBarHeight = navBarHeight, onTabSelected = { selectedTab = it }, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun CurvedNavBar(
    selectedTab: Int,
    navBarHeight: androidx.compose.ui.unit.Dp,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val screenWidth = this.maxWidth
        val scaleFactor = (screenWidth.value / 411f).coerceIn(0.7f, 1.1f)
        
        val scaledBarHeight = (BarContentHeight.value * scaleFactor).dp
        val scaledButtonSize = (ButtonSize.value * scaleFactor).dp
        val scaledButtonCenterY = (ButtonCenterY.value * scaleFactor).dp
        val scaledCircleGap = (CircleGap.value * scaleFactor).dp
        val scaledBarTopY = (BarTopY.value * scaleFactor).dp
        val scaledItemBottomPadding = (ItemBottomPadding.value * scaleFactor).dp

        val tabs = listOf("Dashboard" to Icons.Default.Dashboard, "Settings" to Icons.Default.Settings, "Logs" to Icons.Default.Code, "About" to Icons.Default.Info)
        val tabCount = tabs.size
        var barWidthPx by remember { mutableIntStateOf(0) }
        val indicatorOffset by animateFloatAsState(targetValue = selectedTab.toFloat(), animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "indicatorOffset")

        Box(modifier = Modifier.fillMaxWidth().height(scaledBarHeight + navBarHeight).onSizeChanged { barWidthPx = it.width }) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val tabWidth = size.width / tabCount
                val centerX = (indicatorOffset * tabWidth) + (tabWidth / 2)
                val barTop = scaledBarTopY.toPx()
                val notchBottom = scaledButtonCenterY.toPx() + (scaledButtonSize.toPx() / 2f) + scaledCircleGap.toPx()
                val shoulderWidth = (50.dp.toPx() * scaleFactor)
                val barShape = Path().apply {
                    moveTo(0f, barTop); lineTo(centerX - shoulderWidth, barTop)
                    cubicTo(centerX - (43.dp.toPx() * scaleFactor), barTop, centerX - (40.dp.toPx() * scaleFactor), barTop + (3.dp.toPx() * scaleFactor), centerX - (37.dp.toPx() * scaleFactor), barTop + (11.dp.toPx() * scaleFactor))
                    cubicTo(centerX - (31.dp.toPx() * scaleFactor), barTop + (28.dp.toPx() * scaleFactor), centerX - (21.dp.toPx() * scaleFactor), notchBottom, centerX, notchBottom)
                    cubicTo(centerX + (21.dp.toPx() * scaleFactor), notchBottom, centerX + (31.dp.toPx() * scaleFactor), barTop + (28.dp.toPx() * scaleFactor), centerX + (37.dp.toPx() * scaleFactor), barTop + (11.dp.toPx() * scaleFactor))
                    cubicTo(centerX + (40.dp.toPx() * scaleFactor), barTop + (3.dp.toPx() * scaleFactor), centerX + (43.dp.toPx() * scaleFactor), barTop, centerX + shoulderWidth, barTop)
                    lineTo(size.width, barTop); lineTo(size.width, size.height); lineTo(0f, size.height); close()
                }
                drawPath(path = barShape, color = IosNavBackground.copy(alpha = 0.86f), style = Fill)
            }
            Box(
                modifier = Modifier
                    .size(scaledButtonSize + (scaledCircleGap * 2))
                    .offset {
                        val tabWidth = barWidthPx.toFloat() / tabCount
                        val outerSize = scaledButtonSize.toPx() + scaledCircleGap.toPx() * 2f
                        IntOffset((indicatorOffset * tabWidth + (tabWidth / 2) - (outerSize / 2f)).roundToInt(), (scaledButtonCenterY.toPx() - outerSize / 2f).roundToInt())
                    }, contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(scaledButtonSize).shadow((12 * scaleFactor).dp, CircleShape, spotColor = IosNavActiveBlue).background(IosNavActiveBlue, CircleShape).border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = tabs[selectedTab].second, contentDescription = null, tint = Color.White, modifier = Modifier.size((28 * scaleFactor).dp))
                }
            }
            Row(modifier = Modifier.fillMaxWidth().height(scaledBarHeight).align(Alignment.TopStart), verticalAlignment = Alignment.Bottom) {
                tabs.forEachIndexed { index, (label, icon) ->
                    val isSelected = selectedTab == index
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabSelected(index) }.padding(bottom = scaledItemBottomPadding), contentAlignment = Alignment.BottomCenter) {
                        if (isSelected) {
                            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = (10 * scaleFactor).sp, color = IosNavActiveBlue)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Icon(imageVector = icon, contentDescription = label, tint = IosNavInactiveGrey, modifier = Modifier.size((24 * scaleFactor).dp))
                                Spacer(modifier = Modifier.height((6 * scaleFactor).dp))
                                Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = (10 * scaleFactor).sp, color = IosNavInactiveGrey)
                            }
                        }
                    }
                }
            }
        }
    }
}
