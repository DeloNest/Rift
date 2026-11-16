package com.rift.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Generates Baseline Profiles for Rift Launcher (plan section 8.2).
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = "com.rift.launcher",
        ) {
            pressHome()
            startActivityAndWait()
            // TODO: Add interactions — scroll drawer, open folders, search
        }
    }
}
