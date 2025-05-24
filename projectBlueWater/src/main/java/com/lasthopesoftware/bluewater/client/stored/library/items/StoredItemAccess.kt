package com.lasthopesoftware.bluewater.client.stored.library.items

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem.ItemType
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemHelpers.storedItemType
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetch
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.querydroid.SqLiteAssistants

class StoredItemAccess(private val context: Context) : AccessStoredItems {
	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier): Promise<Boolean> =
		isItemMarkedForSync(libraryId, itemId)
			.eventually { isSynced ->
				val newState = !isSynced
				toggleSync(libraryId, itemId, newState).then {  _ -> newState }
			}

	override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean): Promise<Unit> {
		val inferredItem = inferItem(item)
		return if (enable) enableItemSync(libraryId, inferredItem, StoredItemHelpers.getListType(inferredItem))
		else disableItemSync(libraryId, inferredItem, StoredItemHelpers.getListType(inferredItem))
	}

	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier, enable: Boolean): Promise<Unit> {
		return if (enable) enableItemSync(libraryId, itemId, itemId.storedItemType)
		else disableItemSync(libraryId, itemId, itemId.storedItemType)
	}

	override fun isItemMarkedForSync(libraryId: LibraryId, itemId: KeyedIdentifier): Promise<Boolean> =
		promiseTableMessage<Boolean, StoredItem> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				isItemMarkedForSync(
					repositoryAccessHelper,
					libraryId,
					itemId,
					itemId.storedItemType,
				)
			}
		}

	override fun isItemMarkedForSync(libraryId: LibraryId, item: IItem): Promise<Boolean> =
		promiseTableMessage<Boolean, StoredItem> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				val inferredItem = inferItem(item)
				isItemMarkedForSync(
					repositoryAccessHelper,
					libraryId,
					inferredItem,
					StoredItemHelpers.getListType(inferredItem)
				)
			}
		}

	override fun disableAllLibraryItems(libraryId: LibraryId): Promise<Unit> =
		promiseTableMessage<Unit, StoredItem> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					repositoryAccessHelper
						.mapSql("DELETE FROM ${StoredItem.tableName} WHERE ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}")
						.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
						.execute()
					closeableTransaction.setTransactionSuccessful()
				}
			}
		}

	private fun enableItemSync(libraryId: LibraryId, item: IItem, itemType: ItemType) =
		promiseTableMessage<Unit, StoredItem> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				if (!isItemMarkedForSync(repositoryAccessHelper, libraryId, item, itemType))
					repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
						repositoryAccessHelper
							.mapSql(storedItemInsertSql)
							.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
							.addParameter(StoredItem.serviceIdColumnName, item.key)
							.addParameter(StoredItem.itemTypeColumnName, itemType)
							.execute()
						closeableTransaction.setTransactionSuccessful()
					}
			}
		}

	private fun enableItemSync(libraryId: LibraryId, item: KeyedIdentifier, itemType: ItemType) =
		promiseTableMessage<Unit, StoredItem> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				if (!isItemMarkedForSync(repositoryAccessHelper, libraryId, item, itemType))
					repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
						repositoryAccessHelper
							.mapSql(storedItemInsertSql)
							.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
							.addParameter(StoredItem.serviceIdColumnName, item.id)
							.addParameter(StoredItem.itemTypeColumnName, itemType)
							.execute()
						closeableTransaction.setTransactionSuccessful()
					}
			}
		}

	private fun disableItemSync(libraryId: LibraryId, item: IItem, itemType: ItemType) =
		promiseTableMessage<Unit, StoredItem> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					repositoryAccessHelper
						.mapSql(
							"""
							DELETE FROM ${StoredItem.tableName}
							WHERE ${StoredItem.serviceIdColumnName} = @${StoredItem.serviceIdColumnName}
							AND ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}
							AND ${StoredItem.itemTypeColumnName} = @${StoredItem.itemTypeColumnName}"""
						)
						.addParameter(StoredItem.serviceIdColumnName, item.key)
						.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
						.addParameter(StoredItem.itemTypeColumnName, itemType)
						.execute()
					closeableTransaction.setTransactionSuccessful()
				}
			}
		}

	private fun disableItemSync(libraryId: LibraryId, item: KeyedIdentifier, itemType: ItemType) =
		promiseTableMessage<Unit, StoredItem> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					repositoryAccessHelper
						.mapSql(
							"""
							DELETE FROM ${StoredItem.tableName}
							WHERE ${StoredItem.serviceIdColumnName} = @${StoredItem.serviceIdColumnName}
							AND ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}
							AND ${StoredItem.itemTypeColumnName} = @${StoredItem.itemTypeColumnName}"""
						)
						.addParameter(StoredItem.serviceIdColumnName, item.id)
						.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
						.addParameter(StoredItem.itemTypeColumnName, itemType)
						.execute()
					closeableTransaction.setTransactionSuccessful()
				}
			}
		}

	override fun promiseStoredItems(libraryId: LibraryId): Promise<Collection<StoredItem>> =
		promiseTableMessage<Collection<StoredItem>, StoredItem> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper
					.mapSql("SELECT * FROM ${StoredItem.tableName} WHERE ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}")
					.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
					.fetch()
			}
		}

	companion object {
		private val storedItemInsertSql by lazy {
			SqLiteAssistants.InsertBuilder.fromTable(StoredItem.tableName)
				.addColumn(StoredItem.libraryIdColumnName)
				.addColumn(StoredItem.serviceIdColumnName)
				.addColumn(StoredItem.itemTypeColumnName)
				.buildQuery()
		}

		private fun isItemMarkedForSync(helper: RepositoryAccessHelper, libraryId: LibraryId, item: IItem, itemType: ItemType): Boolean =
			getStoredItem(helper, libraryId, item, itemType) != null

		private fun isItemMarkedForSync(helper: RepositoryAccessHelper, libraryId: LibraryId, item: KeyedIdentifier, itemType: ItemType): Boolean =
			getStoredItem(helper, libraryId, item, itemType) != null

		private fun getStoredItem(helper: RepositoryAccessHelper, libraryId: LibraryId, item: IItem, itemType: ItemType): StoredItem? =
			helper.mapSql("""
					SELECT * FROM ${StoredItem.tableName}
					WHERE ${StoredItem.serviceIdColumnName} = @${StoredItem.serviceIdColumnName}
					AND ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}
					AND ${StoredItem.itemTypeColumnName} = @${StoredItem.itemTypeColumnName}""")
				.addParameter(StoredItem.serviceIdColumnName, item.key)
				.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
				.addParameter(StoredItem.itemTypeColumnName, itemType)
				.fetchFirst()

		private fun getStoredItem(helper: RepositoryAccessHelper, libraryId: LibraryId, item: KeyedIdentifier, itemType: ItemType): StoredItem? =
			helper.mapSql("""
					SELECT * FROM ${StoredItem.tableName}
					WHERE ${StoredItem.serviceIdColumnName} = @${StoredItem.serviceIdColumnName}
					AND ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}
					AND ${StoredItem.itemTypeColumnName} = @${StoredItem.itemTypeColumnName}""")
				.addParameter(StoredItem.serviceIdColumnName, item.id)
				.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
				.addParameter(StoredItem.itemTypeColumnName, itemType)
				.fetchFirst()

		private fun inferItem(item: IItem): IItem {
			if (item is Item) {
				val playlist = item.playlistId
				if (playlist != null) return Playlist(playlist.id)
			}
			return item
		}
	}
}
