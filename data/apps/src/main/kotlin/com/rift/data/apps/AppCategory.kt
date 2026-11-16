package com.rift.data.apps

/**
 * Standard app categories matching Smart Launcher 6 drawer classification.
 */
enum class AppCategory(
    val id: String,
    val titleArabic: String,
    val titleEnglish: String,
    val order: Int,
) {
    GENERAL("general", "العام", "General", 0),
    COMMUNICATION("communication", "التواصل", "Communication", 1),
    INTERNET("internet", "الإنترنت", "Internet", 2),
    GAMES("games", "الألعاب", "Games", 3),
    MEDIA("media", "الوسائط", "Media", 4),
    UTILITIES("utilities", "الأدوات", "Utilities", 5);

    companion object {
        fun fromId(id: String?): AppCategory =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GENERAL
    }
}
