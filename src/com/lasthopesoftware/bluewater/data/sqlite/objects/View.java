package com.lasthopesoftware.bluewater.data.sqlite.objects;

public class View implements ISqliteDefinition {
	private static final String TABLE_NAME = "views";
	private static String[] COLUMNS = { "ID", "LIBRARY_ID", "NAME" };
	private static String[] COLUMN_DEFINITIONS = {	"ID INTEGER PRIMARY KEY",
													"LIBRARY_ID INTEGER", 
													"NAME VARCHAR(50)", 
													"FOREIGN KEY(LIBRARY_ID) REFERENCES LIBRARY(ID)" };
	
	private int id;
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
	
	@Override
	public String[] getSqlColumns() {
		return COLUMNS;
	}
	@Override
	public String[] getSqlColumnDefintions() {
		return COLUMN_DEFINITIONS;
	}
	@Override
	public String getSqlName() {
		return TABLE_NAME;
	}
}
