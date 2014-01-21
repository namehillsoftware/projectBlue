package com.lasthopesoftware.bluewater.data.sqlite.objects;

import java.util.ArrayList;

public class Library {
	private String libraryName;
	private String accessCode;
	private ArrayList<View> views;
	
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
}
