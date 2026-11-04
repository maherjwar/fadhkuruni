package com.example.fadhkuruni

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * حفظ عدادات الأذكار. العدادات تُصفَّر تلقائياً في يوم جديد.
 */
object CounterStore {
    private const val PREFS = "adhkar_counts"
    private const val KEY_DAY = "__day"

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun load(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_DAY, null) != today()) return emptyMap()
        val result = mutableMapOf<String, Int>()
        for ((key, value) in prefs.all) {
            if (key != KEY_DAY && value is Int) result[key] = value
        }
        return result
    }

    fun save(context: Context, key: String, value: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        // إذا تغير اليوم نمسح عدادات الأمس أولاً
        if (prefs.getString(KEY_DAY, null) != today()) editor.clear()
        editor.putString(KEY_DAY, today()).putInt(key, value).apply()
    }
}
