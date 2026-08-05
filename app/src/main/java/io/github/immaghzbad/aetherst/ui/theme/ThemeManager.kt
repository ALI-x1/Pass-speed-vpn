package io.github.immaghzbad.aetherst.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private const val PREFS_NAME = "aetherst_theme_prefs"
private const val KEY_THEME_ID = "selected_theme_id"

// همون تم فعلی پروژه (بنفش تیره) به عنوان پیش‌فرض، تا ظاهر اپ برای
// کاربرهای فعلی عوض نشه.
private const val DEFAULT_THEME_ID = "dark-purple"

/**
 * انتخاب فعلی تم رو نگه می‌داره و بین راه‌اندازی‌های اپ ذخیره می‌کنه.
 * از SharedPreferences استفاده می‌کنه (هیچ کتابخونه‌ی جدیدی لازم نداره).
 */
object ThemeManager {

    var selectedThemeId by mutableStateOf(DEFAULT_THEME_ID)
        private set

    val currentTheme: AppTheme
        get() = AppThemes.find { it.id == selectedThemeId }
            ?: AppThemes.find { it.id == DEFAULT_THEME_ID }
            ?: AppThemes.first()

    private var initialized = false

    /** فقط یک‌بار در ابتدای اجرای اپ (داخل MyApplicationTheme) صدا زده می‌شه */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_THEME_ID, null)
        if (savedId != null && AppThemes.any { it.id == savedId }) {
            selectedThemeId = savedId
        }
    }

    /** برای صدا زدن از صفحه‌ی انتخاب تم، وقتی کاربر تم جدید انتخاب می‌کنه */
    fun setTheme(context: Context, themeId: String) {
        if (AppThemes.none { it.id == themeId }) return
        selectedThemeId = themeId
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_ID, themeId)
            .apply()
    }
}
