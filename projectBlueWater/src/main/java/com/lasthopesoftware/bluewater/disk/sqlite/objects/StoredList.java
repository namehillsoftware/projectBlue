package com.lasthopesoftware.bluewater.disk.sqlite.objects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "StoredLists")
public class StoredList {

	public static final String serviceIdColumnName = "serviceId";

	@DatabaseField(generatedId = true)
	private int id;
	
	@DatabaseField(foreign = true, columnName = "libraryId")
	private Library library;
	
	@DatabaseField
	private int serviceId;
	
	@DatabaseField
	private ListType type;
	
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

	public ListType getType() {
		return type;
	}

	public void setType(ListType type) {
		this.type = type;
	}

	public enum ListType { FILE, PLAYLIST, ITEM }
}