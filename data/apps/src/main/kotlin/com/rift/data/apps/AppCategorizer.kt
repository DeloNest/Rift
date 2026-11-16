package com.rift.data.apps

import android.content.pm.ApplicationInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intelligent categorization engine for apps.
 * Supports Android standard categories, smart heuristics, and persistent user manual overrides.
 */
@Singleton
class AppCategorizer @Inject constructor() {

    private val userOverrides = mutableMapOf<String, AppCategory>()

    /**
     * Sets a manual category override chosen by the user (e.g. moving an app to another tab).
     */
    @Synchronized
    fun setOverride(packageName: String, category: AppCategory) {
        userOverrides[packageName] = category
    }

    /**
     * Sets multiple overrides (e.g. loaded from database).
     */
    @Synchronized
    fun loadOverrides(overrides: Map<String, AppCategory>) {
        userOverrides.putAll(overrides)
    }

    /**
     * Returns the user override if exists, or null.
     */
    @Synchronized
    fun getOverride(packageName: String): AppCategory? = userOverrides[packageName]

    /**
     * Determines the category for an app.
     */
    fun categorize(app: AppInfo): AppCategory {
        // 1. User manual override takes highest priority
        userOverrides[app.packageName]?.let { return it }

        // 2. Android system category (ApplicationInfo.category)
        when (app.category) {
            ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.MEDIA
            ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.COMMUNICATION
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.UTILITIES
            ApplicationInfo.CATEGORY_NEWS -> return AppCategory.INTERNET
            ApplicationInfo.CATEGORY_MAPS -> return AppCategory.UTILITIES
        }

        // 3. Heuristics based on package name & label keywords
        val pkg = app.packageName.lowercase()
        val label = app.label.lowercase()

        // Games
        if (pkg.contains("game") || pkg.contains("play.games") || pkg.contains("supercell") ||
            pkg.contains("ea.") || pkg.contains("gameloft") || pkg.contains("roblox") ||
            pkg.contains("minecraft") || pkg.contains("pubg")) {
            return AppCategory.GAMES
        }

        // Communication
        if (pkg.contains("dialer") || pkg.contains("contacts") || pkg.contains("phone") ||
            pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("signal") ||
            pkg.contains("viber") || pkg.contains("messenger") || pkg.contains("messaging") ||
            pkg.contains("mms") || pkg.contains("sms") || label.contains("اتصال") ||
            label.contains("رسائل") || label.contains("جهات الاتصال")) {
            return AppCategory.COMMUNICATION
        }

        // Media
        if (pkg.contains("camera") || pkg.contains("gallery") || pkg.contains("photos") ||
            pkg.contains("video") || pkg.contains("music") || pkg.contains("spotify") ||
            pkg.contains("netflix") || pkg.contains("youtube") || pkg.contains("player") ||
            pkg.contains("sound") || pkg.contains("recorder") || pkg.contains("vlc") ||
            label.contains("كاميرا") || label.contains("استوديو") || label.contains("موسيقى") ||
            label.contains("فيديو")) {
            return AppCategory.MEDIA
        }

        // Internet
        if (pkg.contains("browser") || pkg.contains("chrome") || pkg.contains("firefox") ||
            pkg.contains("opera") || pkg.contains("edge") || pkg.contains("twitter") ||
            pkg.contains("reddit") || pkg.contains("facebook") || pkg.contains("instagram") ||
            pkg.contains("tiktok") || pkg.contains("mail") || pkg.contains("outlook") ||
            pkg.contains("gmail") || label.contains("متصفح") || label.contains("بريد")) {
            return AppCategory.INTERNET
        }

        // Utilities
        if (pkg.contains("calc") || pkg.contains("clock") || pkg.contains("calendar") ||
            pkg.contains("file") || pkg.contains("notes") || pkg.contains("keep") ||
            pkg.contains("weather") || pkg.contains("tool") || pkg.contains("scanner") ||
            pkg.contains("compass") || label.contains("حاسبة") || label.contains("ساعة") ||
            label.contains("تقويم") || label.contains("ملفاتي") || label.contains("ملاحظات") ||
            label.contains("طقس")) {
            return AppCategory.UTILITIES
        }

        // Default to General
        return AppCategory.GENERAL
    }
}
