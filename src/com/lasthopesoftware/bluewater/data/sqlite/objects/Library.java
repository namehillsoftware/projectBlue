package com.lasthopesoftware.bluewater.data.sqlite.objects;

import java.util.ArrayList;

public class Library implements ISqliteDefinition {
	private static final String NAME = "LIBRARIES";
	private static final String[] COLUMNS = { "ID", "LIBRARY_NAME", "ACCESS_CODE", "AUTH_KEY" };
	private static final String[] COLUMN_DEFINITIONS = { 	"ID INTEGER PRIMARY KEY AUTOINCREMENT",
															"LIBRARY_NAME VARCHAR(50) NOT NULL",
															"ACCESS_CODE VARCHAR(30) NOT NULL",
															"AUTH_KEY VARCHAR VARCHAR(100)" };
	
	private String libraryName;
	private String accessCode;
	private ArrayList<View> views;
	private String authKey;
	
	/**
	 * @return the mLibraryName
	 */
	public String getLibraryName() {
		return libraryName;
	}
	/**
	 * @param mLibraryName the mLibraryName to set
	 */
	public void setLibraryName(String libraryName) {
		this.libraryName = libraryName;
	}
	/**
	 * @return the mAccessCode
	 */
	public String getAccessCode() {
		return accessCode;
	}
	/**
	 * @param mAccessCode the mAccessCode to set
	 */
	public void setAccessCode(String accessCode) {
		this.accessCode = accessCode;
	}
	/**
	 * @return the mViews
	 */
	public ArrayList<View> getViews() {
		return views;
	}
	/**
	 * @param mViews the mViews to set
	 */
	public void setViews(ArrayList<View> views) {
		this.views = views;
	}
	/**
	 * @return the authKey
	 */
	public String getAuthKey() {
		return authKey;
	}
	/**
	 * @param authKey the authKey to set
	 */
	public void setAuthKey(String authKey) {
		this.authKey = authKey;
	}
	@Override
	public String getSqlName() {
		return NAME;
	}
	@Override
	public String[] getSqlColumns() {
		return COLUMNS;
	}
	@Override
	public String[] getSqlColumnDefintions() {
		return COLUMN_DEFINITIONS;
	}
}
