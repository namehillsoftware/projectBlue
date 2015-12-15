package com.lasthopesoftware.bluewater.servers.library.items.repository;

public class StoredItem {

	public static final String tableName = "StoredItems";
	public static final String serviceIdColumnName = "serviceId";
	public static final String libraryIdColumnName = "libraryId";
	public static final String itemTypeColumnName = "itemType";

	private int id;
	
	private int libraryId;

	// unique with library id
	private int serviceId;
	
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

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public enum ItemType { FILE, PLAYLIST, ITEM }
}