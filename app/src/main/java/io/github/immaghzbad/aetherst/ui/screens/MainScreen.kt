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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
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

// ---- رنگ‌های پیش‌فرض Warden (بر اساس تم فایل HTML شما) ----
private val WardenBg = Color(0xFFF0F4FF)
private val WardenBgSub = Color(0xFFE8EEFF)
private val WardenCard = Color(0xFFFFFFFF)
private val WardenCardBorder = Color(0xFFDDEAFF)
private val WardenAccent = Color(0xFF2B82D4)
private val WardenAccentFg = Color(0xFFFFFFFF)
private val WardenText = Color(0xFF0D1B2A)
private val WardenTextMuted = Color(0xFF7A9CC2)
private val WardenSuccess = Color(0xFF22C55E)
private val WardenInputBg = Color(0xFFF5F8FF)
private val WardenInputBorder = Color(0xFFC8DCFF)
private val IosNavActiveBlue = Color(0xFF007AFF)
private val IosNavInactiveGrey = Color(0xFF8E8E93)

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
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity?.window?.isNavigationBarContrastEnforced = false
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
}

@SuppressLint("BatteryLife")
@Composable
private fun DashboardContent(viewModel: AetherViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSplitTunneling by remember { mutableStateOf(false) }
    
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
    var showRoutingRules by remember { mutableStateOf(false) }

    val isConnected = connectionStatus.toString().contains("Connected", ignoreCase = true) 

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

    val saveableStateHolder = rememberSaveableStateHolder()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(WardenBg)) {
        val screenWidth = this.maxWidth
        val scaleFactor = (screenWidth.value / 411f).coerceIn(0.7f, 1.1f)

        Column(modifier = Modifier.fillMaxSize()) {
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WardenCard)
                    .border(1.dp, WardenCardBorder)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WardenAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Logo", tint = WardenAccentFg, modifier = Modifier.size(15.dp))
                    }
                    Text(text = "Warden", color = WardenText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isConnected) WardenSuccess.copy(alpha = 0.1f) else WardenInputBg)
                        .border(1.dp, if (isConnected) WardenSuccess.copy(alpha = 0.3f) else WardenInputBorder, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) WardenSuccess else WardenTextMuted)
                    )
                    Text(
                        text = if (isConnected) "متصل" else "قطع",
                        color = if (isConnected) WardenSuccess else WardenTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).background(WardenBgSub)) {
                val targetScreen = if (showRoutingRules) 100 else if (showSplitTunneling) 99 else selectedTab
                AnimatedContent(
                    targetState = targetScreen,
                    transitionSpec = {
                        val duration = 350
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
                                bottomContentPadding = 0.dp
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
                                bottomContentPadding = 0.dp
                            )
                            4 -> ThemeSelectionScreen(onBack = { selectedTab = 0 })
                            2 -> LogsScreen(viewModel = viewModel, onShowToast = { msg, err -> viewModel.showToast(msg, err) }, bottomContentPadding = 0.dp)
                            3 -> AboutUsScreen(bottomContentPadding = 0.dp)
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
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WardenCard)
                    .border(1.dp, WardenCardBorder)
                    .navigationBarsPadding()
            ) {
                WardenBottomNavItem(
                    title = "داشبورد",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                WardenBottomNavItem(
                    title = "تنظیمات",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
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
private fun WardenBottomNavItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isSelected) WardenAccent else WardenTextMuted.copy(alpha = 0.5f))
        )
        Text(
            text = title,
            color = if (isSelected) WardenAccent else WardenTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
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
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusManager.clearFocus() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width((320 * scaleFactor).dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(IosNavActiveBlue.copy(alpha = 0.1f)),
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = (13 * scaleFactor).sp,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                androidx.compose.foundation.text.BasicTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        color = IosNavActiveBlue,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { 
                        if (code.length == 6) onSubmit(code)
                    }),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(IosNavActiveBlue),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (code.isEmpty()) {
                                Text("000000", color = Color.White.copy(alpha = 0.05f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
                            }
                            innerTextField()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", color = IosNavInactiveGrey, fontWeight = FontWeight.Medium)
                    }
                    androidx.compose.material3.Button(
                        onClick = { if (code.length == 6) onSubmit(code) },
                        enabled = code.length == 6,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = IosNavActiveBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Verify", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
