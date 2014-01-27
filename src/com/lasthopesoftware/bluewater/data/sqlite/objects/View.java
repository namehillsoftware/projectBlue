package com.lasthopesoftware.bluewater.data.sqlite.objects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "VIEWS")
public class View {
	
	@DatabaseField(generatedId = true)
	private Integer id;
	@DatabaseField(columnDefinition = "VARCHAR(50)")
	private String name;
	@DatabaseField(foreign = true, foreignAutoCreate = true)
	private Library library;
	
	/**
	 * @return the key
	 */
	public Integer getId() {
		return id;
	}
	/**
	 * @param key the key to set
	 */
	public void setId(Integer id) {
		this.id = id;
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
}
