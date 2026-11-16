package com.rift.core.perf

import androidx.tracing.trace

/**
 * Convenience wrapper for tracing critical paths.
 */
inline fun <T> riftTrace(label: String, block: () -> T): T = trace(label, block)
