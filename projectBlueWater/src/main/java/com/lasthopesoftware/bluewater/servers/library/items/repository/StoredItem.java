package com.lasthopesoftware.bluewater.servers.library.items.repository;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "StoredItems")
public class StoredItem {

	public static final String serviceIdColumnName = "serviceId";
	public static final String libraryIdColumnName = "libraryId";
	public static final String itemTypeColumnName = "itemType";

	@DatabaseField(generatedId = true)
	private int id;
	
	@DatabaseField(uniqueCombo = true)
	private int libraryId;

	// unique with library id
	@DatabaseField(uniqueCombo = true)
	private int serviceId;
	
	@DatabaseField(uniqueCombo = true)
	private ItemType itemType;
	
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

	public enum ItemType { FILE, PLAYLIST, ITEM }
}