package com.lasthopesoftware.bluewater.servers.library.items.store;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.lasthopesoftware.bluewater.servers.store.Library;

@DatabaseTable(tableName = "StoredItems")
public class StoredItem {

	public static final String serviceIdColumnName = "serviceId";
	public static final String libraryIdColumnName = "libraryId";
	public static final String itemTypeColumnName = "type";

	@DatabaseField(generatedId = true)
	private int id;
	
	@DatabaseField(foreign = true, columnName = "libraryId", uniqueCombo = true)
	private Library library;

	// unique with library id
	@DatabaseField(uniqueCombo = true)
	private int serviceId;
	
	@DatabaseField(uniqueCombo = true)
	private ItemType itemType;
	
	public Library getLibrary() {
		return library;
	}

	public void setLibrary(Library library) {
		this.library = library;
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