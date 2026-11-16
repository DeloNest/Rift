package com.rift.feature.search

import com.rift.data.apps.AppInfo
import javax.inject.Inject
import javax.inject.Singleton

data class SearchableApp(
    val appInfo: AppInfo,
    val normalizedLabel: String,
    val initials: String,
    val packageSegment: String,
    val aliases: List<String> = emptyList(),
)

data class SearchResult(
    val appInfo: AppInfo,
    val score: Int,
)

/**
 * In-memory search index for apps and shortcuts.
 * Designed to process hundreds of apps in < 5ms (P95 target is <= 16ms).
 */
@Singleton
class SearchEngine @Inject constructor() {

    private val index = mutableListOf<SearchableApp>()

    // Common Arabic-English transliteration aliases
    private val defaultAliases = mapOf(
        "whatsapp" to listOf("واتساب", "وتساب", "واتس"),
        "telegram" to listOf("تيليجرام", "تلغرام", "تيلي"),
        "youtube" to listOf("يوتيوب"),
        "twitter" to listOf("تويتر", "اكس"),
        "facebook" to listOf("فيسبوك", "فيس"),
        "instagram" to listOf("انستقرام", "انستغرام"),
        "snapchat" to listOf("سناب", "سنابشات"),
        "tiktok" to listOf("تيكتوك", "تيك"),
        "chrome" to listOf("كروم"),
        "settings" to listOf("اعدادات", "الضبط"),
        "camera" to listOf("كاميرا", "كاميره", "تصوير"),
        "gallery" to listOf("استوديو", "معرض", "صور"),
        "calculator" to listOf("حاسبه", "اله حاسبه"),
        "clock" to listOf("ساعه", "منبه"),
        "messages" to listOf("رسائل", "مسجات"),
        "phone" to listOf("هاتف", "اتصال"),
    )

    @Synchronized
    fun updateApps(apps: List<AppInfo>) {
        index.clear()
        for (app in apps) {
            val normalized = ArabicNormalizer.normalize(app.label)
            val initials = ArabicNormalizer.extractInitials(normalized)
            val pkgSegment = app.packageName.substringAfterLast('.').lowercase()

            val aliases = mutableListOf<String>()
            defaultAliases[pkgSegment]?.forEach { aliases.add(ArabicNormalizer.normalize(it)) }
            for ((key, values) in defaultAliases) {
                if (normalized.contains(key)) {
                    values.forEach { aliases.add(ArabicNormalizer.normalize(it)) }
                }
            }

            index.add(
                SearchableApp(
                    appInfo = app,
                    normalizedLabel = normalized,
                    initials = initials,
                    packageSegment = pkgSegment,
                    aliases = aliases,
                )
            )
        }
    }

    /**
     * Searches indexed apps matching query.
     */
    fun search(query: String, maxResults: Int = 20): List<SearchResult> {
        val normalizedQuery = ArabicNormalizer.normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()

        val results = mutableListOf<SearchResult>()

        for (item in index) {
            var bestScore = FuzzyMatcher.score(normalizedQuery, item.normalizedLabel, item.initials)

            // Try package segment (e.g. "whatsapp")
            val pkgScore = FuzzyMatcher.score(normalizedQuery, item.packageSegment)
            if (pkgScore > bestScore) {
                bestScore = pkgScore - 50 // small penalty for package name match
            }

            // Try aliases
            for (alias in item.aliases) {
                val aliasScore = FuzzyMatcher.score(normalizedQuery, alias)
                if (aliasScore > bestScore) {
                    bestScore = aliasScore
                }
            }

            if (bestScore > FuzzyMatcher.SCORE_NONE) {
                results.add(SearchResult(item.appInfo, bestScore))
            }
        }

        return results
            .sortedByDescending { it.score }
            .take(maxResults)
    }
}
