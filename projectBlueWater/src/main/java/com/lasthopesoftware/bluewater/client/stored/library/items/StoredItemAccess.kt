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
import com.lasthopesoftware.bluewater.repository.fetchFirstOrNull
import com.lasthopesoftware.bluewater.repository.insert
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise

class StoredItemAccess(private val context: Context) : AccessStoredItems {
	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier): Promise<Boolean> =
		isItemMarkedForSync(libraryId, itemId)
			.eventually { isSynced ->
				val newState = !isSynced
				toggleSync(libraryId, itemId, newState).then {  _ -> newState }
			}

	override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean): Promise<Unit> {
		val inferredItem = inferItem(item)
		return if (enable) enableItemSync(libraryId, inferredItem, inferredItem.storedItemType)
		else disableItemSync(libraryId, inferredItem, inferredItem.storedItemType)
	}

	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier, enable: Boolean): Promise<Unit> {
		return if (enable) enableItemSync(libraryId, itemId, itemId.storedItemType)
		else disableItemSync(libraryId, itemId, itemId.storedItemType)
	}

	override fun isItemMarkedForSync(libraryId: LibraryId, itemId: KeyedIdentifier): Promise<Boolean> =
		promiseTableMessage {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.isItemMarkedForSync(
					libraryId,
					itemId,
					itemId.storedItemType,
				)
			}
		}

	override fun isItemMarkedForSync(libraryId: LibraryId, item: IItem): Promise<Boolean> =
		promiseTableMessage {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				val inferredItem = inferItem(item)
				repositoryAccessHelper.isItemMarkedForSync(
					libraryId,
					inferredItem,
					inferredItem.storedItemType
				)
			}
		}

	override fun disableAllLibraryItems(libraryId: LibraryId): Promise<Unit> =
		promiseTableMessage {
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

	override fun updateStoredItemMetadata(libraryId: LibraryId, item: IItem): Promise<Unit> = promiseTableMessage {
		RepositoryAccessHelper(context).use { repositoryAccessHelper ->
			val inferredItem = inferItem(item)
			repositoryAccessHelper.updateStoredItemMetadata(libraryId, inferredItem)
		}
	}

	private fun enableItemSync(libraryId: LibraryId, item: IItem, itemType: ItemType) =
		promiseTableMessage {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				if (!repositoryAccessHelper.isItemMarkedForSync(libraryId, item, itemType))
					repositoryAccessHelper.insert(
						StoredItem.tableName,
						StoredItem(
							libraryId = libraryId.id,
							serviceId = item.key,
							itemType = itemType,
							itemName = item.value,
						),
					)
			}
		}

	private fun enableItemSync(libraryId: LibraryId, item: KeyedIdentifier, itemType: ItemType) =
		promiseTableMessage {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				if (!repositoryAccessHelper.isItemMarkedForSync(libraryId, item, itemType))
					repositoryAccessHelper.insert(
						StoredItem.tableName,
						StoredItem(
							libraryId = libraryId.id,
							serviceId = item.id,
							itemType = itemType,
						),
					)
			}
		}

	private fun disableItemSync(libraryId: LibraryId, item: IItem, itemType: ItemType) =
		promiseTableMessage {
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
		promiseTableMessage {
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
		promiseTableMessage {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper
					.mapSql("SELECT * FROM ${StoredItem.tableName} WHERE ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}")
					.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
					.fetch()
			}
		}

	companion object {
		private fun RepositoryAccessHelper.isItemMarkedForSync(libraryId: LibraryId, item: IItem, itemType: ItemType): Boolean =
			getStoredItem(libraryId, item, itemType) != null

		private fun RepositoryAccessHelper.isItemMarkedForSync(libraryId: LibraryId, item: KeyedIdentifier, itemType: ItemType): Boolean =
			this.getStoredItem(libraryId, item, itemType) != null

		private fun RepositoryAccessHelper.getStoredItem(libraryId: LibraryId, item: IItem, itemType: ItemType): StoredItem? =
			mapSql("""
					SELECT * FROM ${StoredItem.tableName}
					WHERE ${StoredItem.serviceIdColumnName} = @${StoredItem.serviceIdColumnName}
					AND ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}
					AND ${StoredItem.itemTypeColumnName} = @${StoredItem.itemTypeColumnName}""")
				.addParameter(StoredItem.serviceIdColumnName, item.key)
				.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
				.addParameter(StoredItem.itemTypeColumnName, itemType)
				.fetchFirstOrNull()

		private fun RepositoryAccessHelper.getStoredItem(libraryId: LibraryId, item: KeyedIdentifier, itemType: ItemType): StoredItem? =
			mapSql("""
					SELECT * FROM ${StoredItem.tableName}
					WHERE ${StoredItem.serviceIdColumnName} = @${StoredItem.serviceIdColumnName}
					AND ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}
					AND ${StoredItem.itemTypeColumnName} = @${StoredItem.itemTypeColumnName}""")
				.addParameter(StoredItem.serviceIdColumnName, item.id)
				.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
				.addParameter(StoredItem.itemTypeColumnName, itemType)
				.fetchFirstOrNull()

		private fun RepositoryAccessHelper.updateStoredItemMetadata(libraryId: LibraryId, item: IItem) =
			mapSql(
				"""
				UPDATE ${StoredItem.tableName}
				SET ${StoredItem.itemNameColumnName} = @${StoredItem.itemNameColumnName}
				WHERE ${StoredItem.serviceIdColumnName} = @${StoredItem.serviceIdColumnName}
				AND ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}
				AND ${StoredItem.itemTypeColumnName} = @${StoredItem.itemTypeColumnName}
				""")
				.addParameter(StoredItem.itemNameColumnName, item.value)
				.addParameter(StoredItem.serviceIdColumnName, item.key)
				.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
				.addParameter(StoredItem.itemTypeColumnName, item.storedItemType)
				.execute()

		private fun inferItem(item: IItem): IItem {
			if (item is Item) {
				val playlist = item.playlistId
				if (playlist != null) return Playlist(playlist.id, item.value)
			}
			return item
		}
	}
}
