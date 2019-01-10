package com.lasthopesoftware.bluewater.client.sync.library.items;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Keep;
import com.lasthopesoftware.bluewater.repository.IEntityCreator;
import com.lasthopesoftware.bluewater.repository.IEntityUpdater;

@Keep
public class StoredItem implements IEntityCreator, IEntityUpdater {

	public static final String tableName = "StoredItems";
	public static final String serviceIdColumnName = "serviceId";
	public static final String libraryIdColumnName = "libraryId";
	public static final String itemTypeColumnName = "itemType";

	private int id;
	private int libraryId;
	// unique with library id
	private int serviceId;
	private ItemType itemType;

	@SuppressWarnings("unused")
	public StoredItem() {}

	public StoredItem(int libraryId, int serviceId, ItemType itemType) {
		this.libraryId = libraryId;
		this.serviceId = serviceId;
		this.itemType = itemType;
	}
	
	public int getLibraryId() {
		return libraryId;
	}

	public void setLibraryId(int libraryId) {
		this.libraryId = libraryId;
	}

	public int getServiceId() {
		return serviceId;
	}

	public void setServiceId(int serviceId) {
		this.serviceId = serviceId;
	}

	public ItemType getItemType() {
		return itemType;
	}

	public void setItemType(ItemType type) {
		this.itemType = type;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private static final String createTableSql = "CREATE TABLE `StoredItems` (`id` INTEGER PRIMARY KEY AUTOINCREMENT , `itemType` VARCHAR , `libraryId` INTEGER , `serviceId` INTEGER , UNIQUE (`itemType`,`libraryId`,`serviceId`) ) ";

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(createTableSql);
	}

	@Override
	public void onUpdate(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion >= 5) return;

		db.execSQL("DROP TABLE `StoredLists`;");
		db.execSQL(createTableSql);
	}

	public enum ItemType { FILE, PLAYLIST, ITEM }
}