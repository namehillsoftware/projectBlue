package com.lasthopesoftware.bluewater.data.sqlite.objects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "SELECTED_VIEWS")
public class SelectedView {
	
	@DatabaseField(generatedId = true)
	private int id;
	@DatabaseField()
	private int serviceKey;
	@DatabaseField(columnDefinition = "VARCHAR(50)")
	private String name;
	@DatabaseField(foreign = true, foreignAutoCreate = true)
	private Library library;
	
	/**
	 * @return the key
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the serviceKey
	 */
	public int getServiceKey() {
		return serviceKey;
	}
	/**
	 * @param serviceKey the serviceKey to set
	 */
	public void setServiceKey(int serviceKey) {
		this.serviceKey = serviceKey;
	}
	/**
	 * @return the library
	 */
	public Library getLibrary() {
		return library;
	}
	/**
	 * @param library the library to set
	 */
	public void setLibrary(Library library) {
		this.library = library;
	}
}
