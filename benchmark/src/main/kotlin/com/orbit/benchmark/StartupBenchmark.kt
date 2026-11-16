package com.orbit.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import org.junit.Rule
import org.junit.Test

/**
 * Startup benchmark for Orbit Launcher.
 * Measures cold, warm, and hot start times (plan section 8.1).
 */
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCold() = benchmarkRule.measureRepeated(
        packageName = "com.orbit.launcher",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        iterations = 10,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun startupWarm() = benchmarkRule.measureRepeated(
        packageName = "com.orbit.launcher",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        iterations = 10,
        startupMode = StartupMode.WARM,
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun startupHot() = benchmarkRule.measureRepeated(
        packageName = "com.orbit.launcher",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        iterations = 10,
        startupMode = StartupMode.HOT,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
