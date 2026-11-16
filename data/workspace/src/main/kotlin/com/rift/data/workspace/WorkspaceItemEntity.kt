package com.rift.data.workspace

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single item on the workspace (home screen).
 * From plan section 7.1.
 */
@Entity(
    tableName = "workspace_item",
    indices = [Index("container", "page")]
)
data class WorkspaceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** APP = 0, SHORTCUT = 1, FOLDER = 2, WIDGET = 3 */
    val type: Int,
    /** DESKTOP = -100, DOCK = -101, or folderId */
    val container: Long,
    val page: Int,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    /** "pkg/cls" format */
    val componentName: String? = null,
    /** For work profile / secure folder */
    val userSerial: Long = 0,
    val shortcutId: String? = null,
    /** Not transferable between devices (see plan 7.4) */
    val appWidgetId: Int? = null,
    /** For widget restore */
    val widgetProvider: String? = null,
    val title: String? = null,
    val rank: Int = 0,
) {
    companion object {
        const val TYPE_APP = 0
        const val TYPE_SHORTCUT = 1
        const val TYPE_FOLDER = 2
        const val TYPE_WIDGET = 3

        const val CONTAINER_DESKTOP = -100L
        const val CONTAINER_DOCK = -101L
    }
}
