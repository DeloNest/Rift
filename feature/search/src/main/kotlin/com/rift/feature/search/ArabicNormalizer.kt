package com.rift.feature.search

import java.text.Normalizer
import java.util.Locale

/**
 * Normalizer tailored for Arabic and Latin text according to plan.md section 7.5.
 *
 * Rules:
 * - Strips tashkeel (harakat): U+064B–U+065F, U+0670
 * - Strips tatweel (kashida): U+0640
 * - Unifies Alef: أ, إ, آ, ٱ -> ا
 * - Unifies Yaa / Alef Maksura: ى -> ي
 * - Unifies Taa Marbuta: ة -> ه
 * - Normalizes Eastern Arabic numerals (٠-٩) to Western (0-9)
 * - Latin: lowercase + remove accents/diacritics
 */
object ArabicNormalizer {

    private val TASHKEEL_REGEX = Regex("[\u064B-\u065F\u0670]")
    private const val TATWEEL = '\u0640'

    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""

        var text = input.trim()

        // 1. Remove Tashkeel & Tatweel
        text = text.replace(TATWEEL.toString(), "")
        text = text.replace(TASHKEEL_REGEX, "")

        // 2. Unify Arabic letters & convert numerals
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ى' -> sb.append('ي')
                'ة' -> sb.append('ه')
                '٠' -> sb.append('0')
                '١' -> sb.append('1')
                '٢' -> sb.append('2')
                '٣' -> sb.append('3')
                '٤' -> sb.append('4')
                '٥' -> sb.append('5')
                '٦' -> sb.append('6')
                '٧' -> sb.append('7')
                '٨' -> sb.append('8')
                '٩' -> sb.append('9')
                else -> sb.append(ch)
            }
        }

        // 3. Normalize Latin accents & lowercase
        val decomposed = Normalizer.normalize(sb.toString(), Normalizer.Form.NFD)
        val latinClean = decomposed.replace(Regex("\\p{M}"), "")

        return latinClean.lowercase(Locale.ROOT)
    }

    /**
     * Extracts initials / acronyms from a phrase (e.g. "Google Maps" -> "gm", "مدير الملفات" -> "مم").
     */
    fun extractInitials(normalizedText: String): String {
        return normalizedText
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .map { it[0] }
            .joinToString("")
    }
}
