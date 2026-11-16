package com.orbit.feature.search

import kotlin.math.max
import kotlin.math.min

/**
 * High-performance string matching and fuzzy scoring for launcher search.
 * Meets plan section 7.5 target of <= 16ms across hundreds of items.
 */
object FuzzyMatcher {

    const val SCORE_EXACT = 1000
    const val SCORE_PREFIX = 800
    const val SCORE_WORD_PREFIX = 600
    const val SCORE_INITIALS = 500
    const val SCORE_CONTAINS = 300
    const val SCORE_FUZZY = 100
    const val SCORE_NONE = 0

    /**
     * Calculates the match score of query against target.
     * Both query and target must be pre-normalized.
     *
     * Returns a score >= 0. Higher is better. 0 indicates no match.
     */
    fun score(query: String, target: String, initials: String = ""): Int {
        if (query.isEmpty() || target.isEmpty()) return SCORE_NONE

        // 1. Exact match
        if (target == query) {
            return SCORE_EXACT
        }

        // 2. Target starts with query (prefix)
        if (target.startsWith(query)) {
            val lengthPenalty = min(100, (target.length - query.length) * 2)
            return SCORE_PREFIX - lengthPenalty
        }

        // 3. Word prefix (e.g. "Maps" matches "Google Maps")
        val words = target.split(' ')
        for (word in words) {
            if (word.startsWith(query)) {
                return SCORE_WORD_PREFIX
            }
        }

        // 4. Initials match (e.g. "gm" -> "Google Maps")
        if (initials.isNotEmpty() && (initials == query || initials.startsWith(query))) {
            return SCORE_INITIALS
        }

        // 5. Substring containment
        val containsIndex = target.indexOf(query)
        if (containsIndex >= 0) {
            val positionPenalty = min(50, containsIndex * 2)
            return SCORE_CONTAINS - positionPenalty
        }

        // 6. Fuzzy match (Damerau-Levenshtein) for queries >= 4 chars
        if (query.length >= 4) {
            val distance = damerauLevenshtein(query, target.take(query.length + 2))
            val maxAllowedDist = if (query.length >= 6) 2 else 1
            if (distance <= maxAllowedDist) {
                return SCORE_FUZZY - (distance * 30)
            }
        }

        return SCORE_NONE
    }

    /**
     * Calculates Damerau-Levenshtein distance with transposition support.
     */
    fun damerauLevenshtein(source: String, target: String): Int {
        val sLen = source.length
        val tLen = target.length
        if (sLen == 0) return tLen
        if (tLen == 0) return sLen

        val d = Array(sLen + 1) { IntArray(tLen + 1) }

        for (i in 0..sLen) d[i][0] = i
        for (j in 0..tLen) d[0][j] = j

        for (i in 1..sLen) {
            for (j in 1..tLen) {
                val cost = if (source[i - 1] == target[j - 1]) 0 else 1

                d[i][j] = min(
                    d[i - 1][j] + 1, // deletion
                    min(
                        d[i][j - 1] + 1, // insertion
                        d[i - 1][j - 1] + cost // substitution
                    )
                )

                // Transposition
                if (i > 1 && j > 1 && source[i - 1] == target[j - 2] && source[i - 2] == target[j - 1]) {
                    d[i][j] = min(d[i][j], d[i - 2][j - 2] + cost)
                }
            }
        }

        return d[sLen][tLen]
    }
}
