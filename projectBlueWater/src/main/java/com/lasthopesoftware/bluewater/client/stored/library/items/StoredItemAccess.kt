package com.lasthopesoftware.bluewater.client.stored.library.items

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem.ItemType
import com.lasthopesoftware.bluewater.repository.InsertBuilder.Companion.fromTable
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetch
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise

class StoredItemAccess(private val context: Context) : AccessStoredItems {
	override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean) {
		val inferredItem = inferItem(item)
		if (enable) enableItemSync(libraryId, inferredItem, StoredItemHelpers.getListType(inferredItem))
		else disableItemSync(libraryId, inferredItem, StoredItemHelpers.getListType(inferredItem))
	}

	override fun toggleSync(libraryId: LibraryId, itemId: KeyedIdentifier, enable: Boolean): Promise<Unit> {
		val type = when (itemId) {
			is ItemId -> ItemType.ITEM
			is PlaylistId -> ItemType.PLAYLIST
			else -> throw IllegalArgumentException("itemId")
		}

		return if (enable) enableItemSync(libraryId, itemId, type)
		else disableItemSync(libraryId, itemId, type)
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

	private fun enableItemSync(libraryId: LibraryId, item: IItem, itemType: ItemType) {
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
			fromTable(StoredItem.tableName)
				.addColumn(StoredItem.libraryIdColumnName)
				.addColumn(StoredItem.serviceIdColumnName)
				.addColumn(StoredItem.itemTypeColumnName)
				.build()
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
