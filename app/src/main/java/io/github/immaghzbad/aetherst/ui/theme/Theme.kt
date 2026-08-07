package io.github.immaghzbad.aetherst.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // خواندن تم ذخیره‌شده قبل از رندر اولیه
    ThemeManager.init(context)

    val appTheme = ThemeManager.currentTheme
    val colorScheme = appTheme.colorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: context as? Activity
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !appTheme.isDark
                insetsController.isAppearanceLightNavigationBars = !appTheme.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
        LocalAppTheme provides appTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
