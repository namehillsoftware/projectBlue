package com.lasthopesoftware.bluewater.client.stored.library.items

import android.database.sqlite.SQLiteDatabase
import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.IdentifiableEntity
import com.lasthopesoftware.bluewater.repository.IEntityCreator
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import com.lasthopesoftware.bluewater.repository.fetch
import com.namehillsoftware.querydroid.SqLiteAssistants
import com.namehillsoftware.querydroid.SqLiteCommand

@Keep
data class StoredItem(
	override var id: Int = 0,
	var libraryId: Int = 0,
	// unique with library id
	var serviceId: String = "",
	var itemType: ItemType? = null,
	var itemName: String? = null,
) : IdentifiableEntity, IEntityCreator, IEntityUpdater {

	constructor(libraryId: Int, serviceId: String, itemType: ItemType, itemName: String? = null) : this() {
		this.libraryId = libraryId
		this.serviceId = serviceId
		this.itemType = itemType
		this.itemName = itemName
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

		if (oldVersion < 24) {
			db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$itemNameColumnName` VARCHAR")
		}
	}

	@Keep
	enum class ItemType {
		FILE, PLAYLIST, ITEM
	}

	@Keep
	class Version5StoredItem {
		var id = 0
		var libraryId = 0

		// unique with library id
		var serviceId = 0
		lateinit var itemType: ItemType

		fun toStoredItem(): StoredItem {
			return StoredItem(
				libraryId = libraryId,
				serviceId = serviceId.toString(),
				itemType = itemType
			).also { it.id = id }
		}
	}

	companion object {
		const val tableName = "StoredItems"
		const val serviceIdColumnName = "serviceId"
		const val libraryIdColumnName = "libraryId"
		const val itemTypeColumnName = "itemType"
		const val itemNameColumnName = "itemName"
		private const val createTableSql =
			"""CREATE TABLE IF NOT EXISTS `$tableName`(
				`id` INTEGER PRIMARY KEY AUTOINCREMENT ,
				`$itemTypeColumnName` VARCHAR,
				`$libraryIdColumnName` INTEGER,
				`$serviceIdColumnName` VARCHAR,
				`$itemNameColumnName` VARCHAR,
				UNIQUE (`$itemTypeColumnName`,`$libraryIdColumnName`,`$serviceIdColumnName`) ) """

		private fun storedItemsInsertStatement(tableName: String) = SqLiteAssistants.InsertBuilder
			.fromTable(tableName)
			.addColumn("id")
			.addColumn(itemTypeColumnName)
			.addColumn(libraryIdColumnName)
			.addColumn(serviceIdColumnName)
			.buildQuery()
	}
}
