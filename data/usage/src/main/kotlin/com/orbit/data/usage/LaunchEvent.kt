package com.orbit.data.usage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records a single app launch event for the smart prediction engine.
 * Table is capped at ~5,000 rows with compression (plan section 7.6).
 */
@Entity(tableName = "launch_event")
data class LaunchEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hourBucket: Int,
    val dayOfWeek: Int,
    val isCharging: Boolean = false,
    val isHeadsetConnected: Boolean = false,
    val previousApp: String? = null,
)
