package com.lasthopesoftware.bluewater.client.stored.library.items

import android.database.sqlite.SQLiteDatabase
import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.repository.IEntityCreator
import com.lasthopesoftware.bluewater.repository.IEntityUpdater

@Keep
class StoredItem : IEntityCreator, IEntityUpdater {
	var id = 0
	var libraryId = 0

	// unique with library id
	var serviceId = 0
	var itemType: ItemType? = null

	constructor()

	constructor(libraryId: Int, serviceId: Int, itemType: ItemType?) {
		this.libraryId = libraryId
		this.serviceId = serviceId
		this.itemType = itemType
	}

	override fun onCreate(db: SQLiteDatabase) = db.execSQL(createTableSql)

	override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		if (oldVersion >= 5) return
		db.execSQL("DROP TABLE `StoredLists`;")
		db.execSQL(createTableSql)
	}

	@Keep
	enum class ItemType {
		FILE, PLAYLIST, ITEM
	}

	companion object {
		const val tableName = "StoredItems"
		const val serviceIdColumnName = "serviceId"
		const val libraryIdColumnName = "libraryId"
		const val itemTypeColumnName = "itemType"
		const val idColumnName = "id"
		private const val createTableSql = "CREATE TABLE `StoredItems` (`id` INTEGER PRIMARY KEY AUTOINCREMENT , `itemType` VARCHAR , `libraryId` INTEGER , `serviceId` INTEGER , UNIQUE (`itemType`,`libraryId`,`serviceId`) ) "
	}
}
