package com.orbit.core.perf

import androidx.tracing.trace

/**
 * Convenience wrapper for tracing critical paths.
 */
inline fun <T> orbitTrace(label: String, block: () -> T): T = trace(label, block)
