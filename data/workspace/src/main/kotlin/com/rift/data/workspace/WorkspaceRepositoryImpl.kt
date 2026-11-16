package com.rift.data.workspace

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepositoryImpl @Inject constructor(
    private val dao: WorkspaceDao,
) : WorkspaceRepository {

    override fun getDesktopItems(page: Int): Flow<List<WorkspaceItemEntity>> =
        dao.getItemsByPage(WorkspaceItemEntity.CONTAINER_DESKTOP, page)

    override fun getDockItems(): Flow<List<WorkspaceItemEntity>> =
        dao.getItemsByContainer(WorkspaceItemEntity.CONTAINER_DOCK)

    override fun getFolderItems(folderId: Long): Flow<List<WorkspaceItemEntity>> =
        dao.getFolderItems(folderId)

    override fun getAllWidgets(): Flow<List<WorkspaceItemEntity>> =
        dao.getAllWidgets()

    override fun getWidgetsByContainer(container: Long): Flow<List<WorkspaceItemEntity>> =
        dao.getWidgetsByContainer(container)

    override suspend fun getPages(): List<Int> =
        dao.getPages(WorkspaceItemEntity.CONTAINER_DESKTOP)

    override suspend fun addItem(item: WorkspaceItemEntity): Long =
        dao.insert(item)

    override suspend fun updateItem(item: WorkspaceItemEntity) =
        dao.update(item)

    override suspend fun removeItem(id: Long) =
        dao.deleteById(id)

    override suspend fun exportAll(): List<WorkspaceItemEntity> =
        dao.getAllItems()

    override suspend fun importAll(items: List<WorkspaceItemEntity>) =
        dao.replaceAll(items)
}
