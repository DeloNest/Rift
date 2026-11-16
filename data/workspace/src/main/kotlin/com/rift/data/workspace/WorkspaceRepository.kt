package com.rift.data.workspace

import kotlinx.coroutines.flow.Flow

/**
 * Repository for workspace (home screen) items.
 */
interface WorkspaceRepository {
    fun getDesktopItems(page: Int): Flow<List<WorkspaceItemEntity>>
    fun getDockItems(): Flow<List<WorkspaceItemEntity>>
    fun getFolderItems(folderId: Long): Flow<List<WorkspaceItemEntity>>
    fun getAllWidgets(): Flow<List<WorkspaceItemEntity>>
    fun getWidgetsByContainer(container: Long): Flow<List<WorkspaceItemEntity>>
    suspend fun getPages(): List<Int>
    suspend fun addItem(item: WorkspaceItemEntity): Long
    suspend fun updateItem(item: WorkspaceItemEntity)
    suspend fun removeItem(id: Long)
    suspend fun exportAll(): List<WorkspaceItemEntity>
    suspend fun importAll(items: List<WorkspaceItemEntity>)
}
