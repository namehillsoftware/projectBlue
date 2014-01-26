package com.lasthopesoftware.bluewater.data.sqlite.objects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "VIEWS")
public class View {
	private static String[] COLUMNS = { "ID", "LIBRARY_ID", "NAME" };
	private static String[] COLUMN_DEFINITIONS = {	"ID INTEGER PRIMARY KEY",
													"LIBRARY_ID INTEGER", 
													"NAME VARCHAR(50)", 
													"FOREIGN KEY(LIBRARY_ID) REFERENCES LIBRARY(ID)" };
	
	@DatabaseField(id = true, generatedId = true)
	private int id;
	@DatabaseField(columnDefinition = "VARCHAR(50)")
	private String name;
	/**
	 * @return the key
	 */
	public int getId() {
		return id;
	}
	/**
	 * @param key the key to set
	 */
	public void setId(int id) {
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
