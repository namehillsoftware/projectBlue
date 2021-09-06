package com.lasthopesoftware.bluewater.client.stored.library.items

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem.ItemType
import com.lasthopesoftware.bluewater.repository.DatabasePromise
import com.lasthopesoftware.bluewater.repository.InsertBuilder.Companion.fromTable
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetch
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.namehillsoftware.handoff.promises.Promise

class StoredItemAccess(private val context: Context) : IStoredItemAccess {
	override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean) {
		val inferredItem = inferItem(item)
		if (enable) enableItemSync(libraryId, inferredItem, StoredItemHelpers.getListType(inferredItem))
		else disableItemSync(libraryId, inferredItem, StoredItemHelpers.getListType(inferredItem))
	}

	override fun isItemMarkedForSync(libraryId: LibraryId, item: IItem): Promise<Boolean> {
		return DatabasePromise {
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
	}

	override fun disableAllLibraryItems(libraryId: LibraryId): Promise<Unit> =
		DatabasePromise {
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
		DatabasePromise {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				if (isItemMarkedForSync(repositoryAccessHelper, libraryId, item, itemType)) return@DatabasePromise
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

	private fun disableItemSync(libraryId: LibraryId, item: IItem, itemType: ItemType) {
		DatabasePromise {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					repositoryAccessHelper
						.mapSql("""
							DELETE FROM ${StoredItem.tableName}
							WHERE ${StoredItem.serviceIdColumnName} = @${StoredItem.serviceIdColumnName}
							AND ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}
							AND ${StoredItem.itemTypeColumnName} = @${StoredItem.itemTypeColumnName}""")
						.addParameter(StoredItem.serviceIdColumnName, item.key)
						.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
						.addParameter(StoredItem.itemTypeColumnName, itemType)
						.execute()
					closeableTransaction.setTransactionSuccessful()
				}
			}
		}
	}

	override fun promiseStoredItems(libraryId: LibraryId): Promise<Collection<StoredItem>> =
		DatabasePromise {
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

		private fun isItemMarkedForSync(helper: RepositoryAccessHelper, libraryId: LibraryId, item: IItem, itemType: ItemType): Boolean {
			return getStoredItem(helper, libraryId, item, itemType) != null
		}

		private fun getStoredItem(helper: RepositoryAccessHelper, libraryId: LibraryId, item: IItem, itemType: ItemType): StoredItem? {
			return helper.mapSql("""
					SELECT * FROM ${StoredItem.tableName}
					WHERE ${StoredItem.serviceIdColumnName} = @${StoredItem.serviceIdColumnName}
					AND ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}
					AND ${StoredItem.itemTypeColumnName} = @${StoredItem.itemTypeColumnName}""")
				.addParameter(StoredItem.serviceIdColumnName, item.key)
				.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
				.addParameter(StoredItem.itemTypeColumnName, itemType)
				.fetchFirst()
		}

		private fun inferItem(item: IItem): IItem {
			if (item is Item) {
				val playlist = item.playlist
				if (playlist != null) return playlist
			}
			return item
		}
	}
}
