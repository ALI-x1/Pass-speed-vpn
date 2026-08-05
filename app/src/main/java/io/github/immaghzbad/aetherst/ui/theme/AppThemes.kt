package io.github.immaghzbad.aetherst.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * یک پالت کامل: هم رنگ‌های Material3 و هم رنگ‌های اختصاصی اپ
 * (وضعیت اتصال / اسکن / خطا)
 */
data class AppTheme(
    val id: String,
    val label: String,
    val emoji: String,
    val isDark: Boolean,
    val colorScheme: ColorScheme,
    val connected: Color,
    val scanning: Color,
    val error: Color,
)

/* ----------------------------------------------------------------------
 * رنگ‌های عمومی وضعیت (سبز/کهربایی/قرمز) که برای اکثر تم‌ها یکسانه
 * ---------------------------------------------------------------------- */
private val GenericConnected = Color(0xFF2ECC71)
private val GenericScanning = Color(0xFFFFC53D)
private val GenericError = Color(0xFFFF5C5C)

/* ----------------------------------------------------------------------
 * تم فعلی پروژه (بنفش تیره) — رنگ‌هاش دقیقاً همون Elegant* قبلیه،
 * که در Color.kt تعریف شده. این تم برای حفظ ظاهر فعلی اپ نگه داشته می‌شه.
 * ---------------------------------------------------------------------- */
private val DarkPurpleScheme = darkColorScheme(
    primary = ElegantPrimary,
    onPrimary = ElegantOnPrimary,
    primaryContainer = ElegantPrimaryContainer,
    onPrimaryContainer = ElegantOnPrimaryContainer,
    secondary = ElegantSecondary,
    background = ElegantBackground,
    onBackground = ElegantTextPrimary,
    surface = ElegantSurface,
    onSurface = ElegantTextPrimary,
    surfaceVariant = ElegantSurfaceCard,
    onSurfaceVariant = ElegantTextSecondary,
    outline = ElegantOutline,
)

/* ----------------------------------------------------------------------
 * ۵ تم روشن
 * ---------------------------------------------------------------------- */

private val LightYellowScheme = lightColorScheme(
    primary = Color(0xFFC98F1E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE9B0),
    onPrimaryContainer = Color(0xFF5C4300),
    secondary = Color(0xFF8A7549),
    background = Color(0xFFFFFBEF),
    onBackground = Color(0xFF2A2107),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2A2107),
    surfaceVariant = Color(0xFFFFF3D6),
    onSurfaceVariant = Color(0xFF6E5A26),
    outline = Color(0xFFE6D293),
)

private val LightGreenScheme = lightColorScheme(
    primary = Color(0xFF22A85A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8F5E4),
    onPrimaryContainer = Color(0xFF146B39),
    secondary = Color(0xFF4E8067),
    background = Color(0xFFF0FBF4),
    onBackground = Color(0xFF0A1F12),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A1F12),
    surfaceVariant = Color(0xFFE3F8EA),
    onSurfaceVariant = Color(0xFF3D6B4E),
    outline = Color(0xFFAEDFC1),
)

private val LightBlueScheme = lightColorScheme(
    primary = Color(0xFF2B82D4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0EEFF),
    onPrimaryContainer = Color(0xFF1A5FA0),
    secondary = Color(0xFF4E6E8F),
    background = Color(0xFFF0F4FF),
    onBackground = Color(0xFF0D1B2A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0D1B2A),
    surfaceVariant = Color(0xFFE8EEFF),
    onSurfaceVariant = Color(0xFF2C4A6E),
    outline = Color(0xFFC8DCFF),
)

private val LightPinkScheme = lightColorScheme(
    primary = Color(0xFFE0507A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFCE0EA),
    onPrimaryContainer = Color(0xFF8F2748),
    secondary = Color(0xFF8F5E6C),
    background = Color(0xFFFFF3F7),
    onBackground = Color(0xFF2A0D16),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2A0D16),
    surfaceVariant = Color(0xFFFCE4EC),
    onSurfaceVariant = Color(0xFF7A4A57),
    outline = Color(0xFFF2C1D2),
)

private val LightPurpleScheme = lightColorScheme(
    primary = Color(0xFF8A4FD1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECE0FB),
    onPrimaryContainer = Color(0xFF4E2287),
    secondary = Color(0xFF6E5C87),
    background = Color(0xFFF6F0FF),
    onBackground = Color(0xFF1D0F2A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D0F2A),
    surfaceVariant = Color(0xFFEDE3FA),
    onSurfaceVariant = Color(0xFF5C4B75),
    outline = Color(0xFFD5C1F0),
)

/* ----------------------------------------------------------------------
 * ۴ تم تیره‌ی دیگر (زرد/سبز/آبی/صورتی) — بنفش تیره در بالا تعریف شد
 * ---------------------------------------------------------------------- */

private val DarkYellowScheme = darkColorScheme(
    primary = Color(0xFFFFC53D),
    onPrimary = Color(0xFF3D2E00),
    primaryContainer = Color(0xFF5C4600),
    onPrimaryContainer = Color(0xFFFFE7A6),
    secondary = Color(0xFFD8C6A0),
    background = Color(0xFF16110A),
    onBackground = Color(0xFFECE1CB),
    surface = Color(0xFF16110A),
    onSurface = Color(0xFFECE1CB),
    surfaceVariant = Color(0xFF241C0E),
    onSurfaceVariant = Color(0xFFD1BE95),
    outline = Color(0xFF4E4020),
)

private val DarkGreenScheme = darkColorScheme(
    primary = Color(0xFF2ECC71),
    onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF00521A),
    onPrimaryContainer = Color(0xFFACF7B2),
    secondary = Color(0xFFB7CCB4),
    background = Color(0xFF0A140C),
    onBackground = Color(0xFFDDE5DA),
    surface = Color(0xFF0A140C),
    onSurface = Color(0xFFDDE5DA),
    surfaceVariant = Color(0xFF16241A),
    onSurfaceVariant = Color(0xFFBECBB9),
    outline = Color(0xFF3B4A3D),
)

private val DarkBlueScheme = darkColorScheme(
    primary = Color(0xFF4FA3E3),
    onPrimary = Color(0xFF00325A),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD0E4FF),
    secondary = Color(0xFFBBC7DB),
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF0B0F14),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF161C24),
    onSurfaceVariant = Color(0xFFB6C4D6),
    outline = Color(0xFF3A4653),
)

private val DarkPinkScheme = darkColorScheme(
    primary = Color(0xFFFF6F94),
    onPrimary = Color(0xFF5A0021),
    primaryContainer = Color(0xFF7D0030),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFFE0BAC5),
    background = Color(0xFF160A0E),
    onBackground = Color(0xFFEBDDE1),
    surface = Color(0xFF160A0E),
    onSurface = Color(0xFFEBDDE1),
    surfaceVariant = Color(0xFF261620),
    onSurfaceVariant = Color(0xFFD3B7C0),
    outline = Color(0xFF4E3540),
)

/* ----------------------------------------------------------------------
 * لیست کامل تم‌ها — ترتیب دقیقاً مثل طرحی که فرستادی
 * ---------------------------------------------------------------------- */
val AppThemes: List<AppTheme> = listOf(
    AppTheme("light-yellow", "زرد روشن", "🌕", false, LightYellowScheme, GenericConnected, GenericScanning, GenericError),
    AppTheme("light-green", "سبز روشن", "🟢", false, LightGreenScheme, GenericConnected, GenericScanning, GenericError),
    AppTheme("light-blue", "آبی روشن", "🔵", false, LightBlueScheme, GenericConnected, GenericScanning, GenericError),
    AppTheme("light-pink", "صورتی روشن", "🌸", false, LightPinkScheme, GenericConnected, GenericScanning, GenericError),
    AppTheme("light-purple", "بنفش روشن", "🟣", false, LightPurpleScheme, GenericConnected, GenericScanning, GenericError),

    AppTheme("dark-yellow", "زرد تیره", "🌙", true, DarkYellowScheme, GenericConnected, GenericScanning, GenericError),
    AppTheme("dark-green", "سبز تیره", "🌲", true, DarkGreenScheme, GenericConnected, GenericScanning, GenericError),
    AppTheme("dark-blue", "آبی تیره", "🌊", true, DarkBlueScheme, GenericConnected, GenericScanning, GenericError),
    AppTheme("dark-pink", "صورتی تیره", "🌹", true, DarkPinkScheme, GenericConnected, GenericScanning, GenericError),
    // تم پیش‌فرض فعلی پروژه (بدون تغییر رنگ)
    AppTheme("dark-purple", "بنفش تیره", "🔮", true, DarkPurpleScheme, ConnectedGreen, ScanningAmber, ErrorRed),
)

/** برای دسترسی به رنگ‌های اختصاصی هر تم از داخل هر Composable */
val LocalAppTheme = staticCompositionLocalOf { AppThemes.last() }
