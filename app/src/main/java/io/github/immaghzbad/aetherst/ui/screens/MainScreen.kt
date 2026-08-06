package io.github.immaghzbad.aetherst.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.immaghzbad.aetherst.model.OnboardingStep
import io.github.immaghzbad.aetherst.ui.AetherViewModel
import io.github.immaghzbad.aetherst.ui.OnboardingViewModel
import io.github.immaghzbad.aetherst.ui.components.IosToast
import io.github.immaghzbad.aetherst.ui.theme.ThemeSelectionScreen
import kotlin.math.roundToInt

// ثابت‌های ثابت‌سازی رابط کاربری iOS/Material
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

@SuppressLint("BatteryLife")
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
    val crashLog by viewModel.crashLog.collectAsStateWithLifecycle()
    val currentStep by rememberUpdatedState(onboardingState.currentStep)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (currentStep == OnboardingStep.BATTERY_OPTIMIZATION && context.isIgnoringBatteryOptimizations()) {
                    onboardingViewModel.moveToNextStep()
                }
                viewModel.checkBatteryOptimizationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val toastState by viewModel.toastState.collectAsStateWithLifecycle()

    SideEffect {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity?.window?.isNavigationBarContrastEnforced = false
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = this.maxWidth
        val scaleFactor = (screenWidth.value / 411f).coerceIn(0.7f, 1.1f)

        if (!isOnboardingComplete) {
            val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val intent = VpnService.prepare(context)
                if (intent == null) onboardingViewModel.moveToNextStep()
            }
            val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) onboardingViewModel.moveToNextStep() else onboardingViewModel.showNotificationError()
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
                                data = "package:${context.packageName}".toUri()
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
        } else if (crashLog != null) {
            CrashReportScreen(
                crashLog = crashLog!!,
                onRestart = { viewModel.clearCrashLog() },
                onShowToast = { viewModel.showToast(it) }
            )
        } else if (updateInfo != null) {
            UpdateScreen(
                info = updateInfo!!,
                onDismiss = { viewModel.dismissUpdate() },
                scaleFactor = scaleFactor
            )
        } else {
            DashboardContent(viewModel)
        }

        IosToast(
            message = toastState?.message,
            isError = toastState?.isError ?: false,
            scaleFactor = scaleFactor
        )
    }
}

@SuppressLint("BatteryLife")
@Composable
private fun DashboardContent(viewModel: AetherViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSplitTunneling by remember { mutableStateOf(false) }
    var showRoutingRules by remember { mutableStateOf(false) }

    val config by viewModel.config.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val sessionTraffic by viewModel.sessionTraffic.collectAsStateWithLifecycle()
    val ipInfo by viewModel.ipInfo.collectAsStateWithLifecycle()
    val pingState by viewModel.pingState.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isBatteryOptimized by viewModel.isBatteryOptimized.collectAsStateWithLifecycle()
    val importConflictRules by viewModel.importConflictRules.collectAsStateWithLifecycle()
    val importErrorMessage by viewModel.importErrorMessage.collectAsStateWithLifecycle()
    val isOptimizingMtu by viewModel.isOptimizingMtu.collectAsStateWithLifecycle()
    val isWaitingForLoginCode by viewModel.isWaitingForLoginCode.collectAsStateWithLifecycle()
    val scrollToZeroTrust by viewModel.scrollToZeroTrust.collectAsStateWithLifecycle()

    LaunchedEffect(scrollToZeroTrust) {
        if (scrollToZeroTrust) {
            selectedTab = 1
            showSplitTunneling = false
            showRoutingRules = false
        }
    }

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
            viewModel.showToast("Notification permission required", true)
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
    val bottomPadding = BarContentHeight + navBarHeight

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = this.maxWidth
        val scaleFactor = (screenWidth.value / 411f).coerceIn(0.7f, 1.1f)
        val isSubScreenOpen = showSplitTunneling || showRoutingRules

        Box(modifier = Modifier.fillMaxSize()) {
            val targetScreen = if (showRoutingRules) 100 else if (showSplitTunneling) 99 else selectedTab
            
            AnimatedContent(
                targetState = targetScreen,
                transitionSpec = {
                    val duration = 300
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { it } + fadeIn(animationSpec = tween(duration)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { -it / 2 } + fadeOut(animationSpec = tween(duration)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { -it } + fadeIn(animationSpec = tween(duration)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { it / 2 } + fadeOut(animationSpec = tween(duration)))
                    }
                },
                label = "screen_transition"
            ) { tab ->
                saveableStateHolder.SaveableStateProvider(tab) {
                    when (tab) {
                        0 -> DashboardScreen(
                            config = config,
                            connectionStatus = connectionStatus,
                            elapsedSeconds = elapsedSeconds,
                            sessionTraffic = sessionTraffic,
                            ipInfo = ipInfo,
                            pingState = pingState,
                            onToggleVpn = { handleVpnToggle() },
                            onUpdateProtocol = { proto -> viewModel.updateConfig(config.copy(protocol = proto)) },
                            onOpenSettings = { selectedTab = 1 },
                            onOpenThemes = { selectedTab = 4 },
                            onRefreshIpInfo = { viewModel.refreshIpInfo() },
                            onRefreshPing = { viewModel.refreshPing() },
                            onShowToast = { msg, err -> viewModel.showToast(msg, err) },
                            bottomContentPadding = bottomPadding
                        )
                        1 -> SettingsScreen(
                            config = config,
                            isBatteryOptimized = isBatteryOptimized,
                            onBack = { selectedTab = 0 },
                            scrollToSection = scrollToZeroTrust,
                            onSectionScrolled = { viewModel.onZeroTrustScrolled() },
                            onUpdateConfig = { viewModel.updateConfig(it) },
                            onUpdateTunnelEngine = { viewModel.updateTunnelEngine(it) },
                            onApplyPreset = { preset -> viewModel.applyPreset(preset) },
                            onOpenSplitTunneling = { showSplitTunneling = true },
                            onOpenRoutingRules = { showRoutingRules = true },
                            onResetAll = { viewModel.resetAllSettings() },
                            onExportBackup = { viewModel.exportFullBackup(context) },
                            onImportBackup = { viewModel.importFullBackup(it, context) },
                            onOptimizeMtu = { viewModel.optimizeMtu() },
                            isOptimizingMtu = isOptimizingMtu,
                            onRequestBatteryOptimization = {
                                runCatching {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = "package:${context.packageName}".toUri()
                                    }
                                    context.startActivity(intent)
                                }.onFailure {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            },
                            onShowToast = { msg, err -> viewModel.showToast(msg, err) },
                            bottomContentPadding = bottomPadding
                        )
                        4 -> ThemeSelectionScreen(onBack = { selectedTab = 0 })
                        2 -> LogsScreen(viewModel = viewModel, onShowToast = { msg, err -> viewModel.showToast(msg, err) }, bottomContentPadding = bottomPadding)
                        3 -> AboutUsScreen(bottomContentPadding = bottomPadding)
                        99 -> SplitTunnelingScreen(
                            apps = installedApps,
                            excludedPackages = config.excludedPackages,
                            blockedPackages = config.blockedPackages,
                            onUpdateMode = { pkg, mode -> viewModel.updateAppSplitTunnelingMode(pkg, mode) },
                            onBack = { showSplitTunneling = false },
                            scaleFactor = scaleFactor
                        )
                        100 -> RoutingRulesScreen(
                            rules = config.routingRules,
                            importConflictRules = importConflictRules,
                            importErrorMessage = importErrorMessage,
                            onAddRule = { pattern, mode -> viewModel.addRoutingRule(pattern, mode) },
                            onRemoveRule = { pattern -> viewModel.removeRoutingRule(pattern) },
                            onUpdateMode = { pattern, mode -> viewModel.updateRoutingRuleMode(pattern, mode) },
                            onClearAllRules = { viewModel.clearAllRoutingRules() },
                            onCleanPattern = { viewModel.cleanRoutingPattern(it) },
                            onValidatePattern = { viewModel.isValidRoutingPattern(it) },
                            onExportRules = { viewModel.exportRoutingRules(context) },
                            onImportRules = { viewModel.importRoutingRules(it, context) },
                            onResolveConflict = { rules, replace -> viewModel.resolveConflict(rules, replace) },
                            onCancelImport = { viewModel.cancelImport() },
                            onClearImportError = { viewModel.clearImportError() },
                            onShowToast = { msg, err -> viewModel.showToast(msg, err) },
                            onBack = { showRoutingRules = false },
                            scaleFactor = scaleFactor
                        )
                    }
                }
            }

            // نمایش سفارشی نوار ناوبری متحرک منحنی (خفیف شدن در صفحات جانبی)
            AnimatedVisibility(
                visible = !isSubScreenOpen && selectedTab != 4,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                CurvedNavBar(
                    selectedTab = selectedTab,
                    navBarHeight = navBarHeight,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        if (isWaitingForLoginCode) {
            ZeroTrustLoginDialog(
                onSubmit = { viewModel.submitLoginCode(it) },
                onDismiss = { viewModel.submitLoginCode("") },
                scaleFactor = scaleFactor
            )
        }
    }
}

@Composable
fun ZeroTrustLoginDialog(
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    scaleFactor: Float
) {
    var code by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusManager.clearFocus() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width((320 * scaleFactor).dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1C1C1E))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(IosNavActiveBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = IosNavActiveBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Zero Trust Login",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = (20 * scaleFactor).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A one-time code was sent to your email. Please enter it below to authorize this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IosNavInactiveGrey,
                    textAlign = TextAlign.Center,
                    fontSize = (13 * scaleFactor).sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                BasicTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        color = IosNavActiveBlue,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (code.length == 6) onSubmit(code)
                    }),
                    cursorBrush = SolidColor(IosNavActiveBlue),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (code.isEmpty()) {
                                Text(
                                    "000000",
                                    color = Color.White.copy(alpha = 0.1f),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 8.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", color = IosNavInactiveGrey, fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = { if (code.length == 6) onSubmit(code) },
                        enabled = code.length == 6,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IosNavActiveBlue,
                            disabledContainerColor = IosNavActiveBlue.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Verify", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurvedNavBar(
    selectedTab: Int,
    navBarHeight: Dp,
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

        val tabs = listOf(
            "Dashboard" to Icons.Default.Dashboard,
            "Settings" to Icons.Default.Settings,
            "Logs" to Icons.Default.Code,
            "About" to Icons.Default.Info
        )
        val tabCount = tabs.size
        var barWidthPx by remember { mutableIntStateOf(0) }

        val indicatorOffset by animateFloatAsState(
            targetValue = selectedTab.toFloat(),
            animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
            label = "indicatorOffset"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(scaledBarHeight + navBarHeight)
                .onSizeChanged { barWidthPx = it.width }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = (16 * scaleFactor).dp, spotColor = Color.Black.copy(alpha = 0.6f))
            ) {
                val tabWidth = size.width / tabCount
                val centerX = (indicatorOffset * tabWidth) + (tabWidth / 2)
                val barTop = scaledBarTopY.toPx()
                val notchBottom = scaledButtonCenterY.toPx() + (scaledButtonSize.toPx() / 2f) + scaledCircleGap.toPx()
                val shoulderWidth = (45.dp.toPx() * scaleFactor)

                val barShape = Path().apply {
                    moveTo(0f, barTop)
                    lineTo(centerX - shoulderWidth, barTop)

                    cubicTo(
                        centerX - (40.dp.toPx() * scaleFactor), barTop,
                        centerX - (38.dp.toPx() * scaleFactor), barTop + (2.dp.toPx() * scaleFactor),
                        centerX - (35.dp.toPx() * scaleFactor), barTop + (10.dp.toPx() * scaleFactor)
                    )
                    cubicTo(
                        centerX - (28.dp.toPx() * scaleFactor), barTop + (26.dp.toPx() * scaleFactor),
                        centerX - (20.dp.toPx() * scaleFactor), notchBottom,
                        centerX, notchBottom
                    )
                    cubicTo(
                        centerX + (20.dp.toPx() * scaleFactor), notchBottom,
                        centerX + (28.dp.toPx() * scaleFactor), barTop + (26.dp.toPx() * scaleFactor),
                        centerX + (35.dp.toPx() * scaleFactor), barTop + (10.dp.toPx() * scaleFactor)
                    )
                    cubicTo(
                        centerX + (38.dp.toPx() * scaleFactor), barTop + (2.dp.toPx() * scaleFactor),
                        centerX + (40.dp.toPx() * scaleFactor), barTop,
                        centerX + shoulderWidth, barTop
                    )

                    lineTo(size.width, barTop)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = barShape,
                    color = IosNavBackground.copy(alpha = 0.96f),
                    style = Fill
                )
            }

            Box(
                modifier = Modifier
                    .size(scaledButtonSize + (scaledCircleGap * 2))
                    .offset {
                        val tabWidth = barWidthPx.toFloat() / tabCount
                        val outerSize = scaledButtonSize.toPx() + scaledCircleGap.toPx() * 2f
                        IntOffset(
                            (indicatorOffset * tabWidth + (tabWidth / 2) - (outerSize / 2f)).roundToInt(),
                            (scaledButtonCenterY.toPx() - outerSize / 2f).roundToInt()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(scaledButtonSize)
                        .shadow(
                            elevation = (12 * scaleFactor).dp,
                            shape = CircleShape,
                            spotColor = IosNavActiveBlue.copy(alpha = 0.6f)
                        )
                        .background(IosNavActiveBlue, CircleShape)
                        .border(
                            width = (1.5 * scaleFactor).dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tabs[selectedTab.coerceIn(0, tabs.size - 1)].second,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size((28 * scaleFactor).dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scaledBarHeight)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.Bottom
            ) {
                tabs.forEachIndexed { index, (label, icon) ->
                    val isSelected = selectedTab == index

                    val contentAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.5f,
                        label = "contentAlpha"
                    )

                    val textOffset by animateFloatAsState(
                        targetValue = if (isSelected) 0f else 8f,
                        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
                        label = "textOffset"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(index) }
                            .padding(bottom = scaledItemBottomPadding),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.graphicsLayer(alpha = contentAlpha)
                        ) {
                            if (!isSelected) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = IosNavInactiveGrey,
                                    modifier = Modifier.size((24 * scaleFactor).dp)
                                )
                                Spacer(modifier = Modifier.height((4 * scaleFactor).dp))
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = (10 * scaleFactor).sp,
                                color = if (isSelected) IosNavActiveBlue else IosNavInactiveGrey,
                                modifier = Modifier.graphicsLayer(translationY = textOffset)
                            )
                        }
                    }
                }
            }
        }
    }
}
