package com.lasthopesoftware.bluewater.data.sqlite.objects;

import android.util.SparseArray;

public class Libraries {
	private String mLibraryName;
	private String mAccessCode;
	private SparseArray<String> mViews;
	
	/**
	 * @return the mLibraryName
	 */
	public String getLibraryName() {
		return mLibraryName;
	}
	/**
	 * @param mLibraryName the mLibraryName to set
	 */
	public void setLibraryName(String mLibraryName) {
		this.mLibraryName = mLibraryName;
	}
	/**
	 * @return the mAccessCode
	 */
	public String getAccessCode() {
		return mAccessCode;
	}
	/**
	 * @param mAccessCode the mAccessCode to set
	 */
	public void setAccessCode(String mAccessCode) {
		this.mAccessCode = mAccessCode;
	}
	/**
	 * @return the mViews
	 */
	public SparseArray<String> getViews() {
		return mViews;
	}
	/**
	 * @param mViews the mViews to set
	 */
	public void setViews(SparseArray<String> mViews) {
		this.mViews = mViews;
	}
}
