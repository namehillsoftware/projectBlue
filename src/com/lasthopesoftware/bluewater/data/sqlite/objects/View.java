package com.lasthopesoftware.bluewater.data.sqlite.objects;

public class View implements ISqliteDefinition {
	private static final String TABLE_NAME = "views";
	private static String[] COLUMNS = { "ID", "NAME" };
	private static String[] COLUMN_DEFINITIONS = { "ID INTEGER PRIMARY KEY", "NAME VARCHAR(50)" };
	
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
	public String[] getTableColumns() {
		return COLUMNS;
	}
	@Override
	public String[] getTableColumnDefintions() {
		return COLUMN_DEFINITIONS;
	}
	@Override
	public String getTableName() {
		return TABLE_NAME;
	}
}
