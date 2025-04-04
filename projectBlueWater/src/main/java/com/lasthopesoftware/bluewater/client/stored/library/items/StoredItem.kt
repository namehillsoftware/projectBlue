package com.lasthopesoftware.bluewater.client.stored.library.items

import android.database.sqlite.SQLiteDatabase
import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.IdentifiableEntity
import com.lasthopesoftware.bluewater.repository.IEntityCreator
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import com.lasthopesoftware.bluewater.repository.InsertBuilder
import com.lasthopesoftware.bluewater.repository.fetch
import com.namehillsoftware.querydroid.SqLiteCommand

@Keep
class StoredItem : IdentifiableEntity, IEntityCreator, IEntityUpdater {
	override var id = 0
	var libraryId = 0

	// unique with library id
	var serviceId = ""
	var itemType: ItemType? = null

	constructor()

	constructor(libraryId: Int, serviceId: String, itemType: ItemType) {
		this.libraryId = libraryId
		this.serviceId = serviceId
		this.itemType = itemType
	}

	override fun onCreate(db: SQLiteDatabase) = db.execSQL(createTableSql)

	override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		if (oldVersion < 5) {
			db.execSQL("DROP TABLE `StoredLists`;")
			db.execSQL(createTableSql)
			return
		}

		if (oldVersion < 20) {
			val tempTableName = tableName + "Temp"
			db.execSQL("DROP TABLE IF EXISTS `$tempTableName`")
			val createTempTableSql = createTableSql.replaceFirst("`$tableName`", "`$tempTableName`")
			db.execSQL(createTempTableSql)
			val storedItems = SqLiteCommand(db, "SELECT * FROM $tableName").fetch<Version5StoredItem>()
			val storedItemsInsertStatement = storedItemsInsertStatement(tempTableName)
			for (storedItem in storedItems) {
				val newStoredItem = storedItem.toStoredItem()

				SqLiteCommand(db, storedItemsInsertStatement)
					.addParameter("id", newStoredItem.id)
					.addParameter(serviceIdColumnName, newStoredItem.serviceId)
					.addParameter(itemTypeColumnName, newStoredItem.itemType)
					.addParameter(libraryIdColumnName, newStoredItem.libraryId)
					.execute()
			}

			db.execSQL("DROP TABLE `$tableName`")
			db.execSQL("ALTER TABLE `$tempTableName` RENAME TO `$tableName`")
		}
	}

	@Keep
	enum class ItemType {
		FILE, PLAYLIST, ITEM
	}

	class Version5StoredItem {
		var libraryId = 0

		// unique with library id
		var serviceId = 0
		lateinit var itemType: ItemType

		fun toStoredItem(): StoredItem {
			return StoredItem(
				libraryId = libraryId,
				serviceId = serviceId.toString(),
				itemType = itemType
			)
		}
	}

	companion object {
		const val tableName = "StoredItems"
		const val serviceIdColumnName = "serviceId"
		const val libraryIdColumnName = "libraryId"
		const val itemTypeColumnName = "itemType"
		private const val createTableSql = "CREATE TABLE IF NOT EXISTS `StoredItems` (`id` INTEGER PRIMARY KEY AUTOINCREMENT , `itemType` VARCHAR , `libraryId` INTEGER , `serviceId` VARCHAR , UNIQUE (`itemType`,`libraryId`,`serviceId`) ) "

		private fun storedItemsInsertStatement(tableName: String) = InsertBuilder
			.fromTable(tableName)
			.addColumn("id")
			.addColumn(itemTypeColumnName)
			.addColumn(libraryIdColumnName)
			.addColumn(serviceIdColumnName)
			.build()
	}
}
