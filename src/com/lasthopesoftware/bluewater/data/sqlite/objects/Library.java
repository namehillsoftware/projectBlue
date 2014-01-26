package com.lasthopesoftware.bluewater.data.sqlite.objects;

import java.util.ArrayList;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "LIBRARIES")
public class Library {
	private static final String[] COLUMNS = { "ID", "LIBRARY_NAME", "ACCESS_CODE", "AUTH_KEY", "NOW_PLAYING_ID", "NOW_PLAYING_PROGRESS" };
	private static final String[] COLUMN_DEFINITIONS = { 	"ID INTEGER PRIMARY KEY AUTOINCREMENT",
															"LIBRARY_NAME VARCHAR(50) NOT NULL",
															"ACCESS_CODE VARCHAR(30) NOT NULL",
															"AUTH_KEY VARCHAR VARCHAR(100)",
															"NOW_PLAYING_ID INTEGER",
															"NOW_PLAYING_PROGRESS INTEGER" };
	
	@DatabaseField(id = true, generatedId= true)
	private int id;
	@DatabaseField(canBeNull = false, columnName = "LIBRARY_NAME", columnDefinition = "VARCHAR(50)")
	private String libraryName;
	@DatabaseField(canBeNull = false, columnName = "ACCESS_CODE", columnDefinition = "VARCHAR(30)")
	private String accessCode;
	
	private String authKey;
	private int nowPlayingId;
	private int nowPlayingProgress;
	
	private View[] views;
	private int[] savedTracks;
	
	/**
	 * @return the nowPlayingId
	 */
	public int getNowPlayingId() {
		return nowPlayingId;
	}
	/**
	 * @param nowPlayingId the nowPlayingId to set
	 */
	public void setNowPlayingId(int nowPlayingId) {
		this.nowPlayingId = nowPlayingId;
	}
	
	
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
	public View[] getViews() {
		return views;
	}
	/**
	 * @param mViews the mViews to set
	 */
	public void setViews(View[] views) {
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

	/**
	 * @return the nowPlayingProgress
	 */
	public int getNowPlayingProgress() {
		return nowPlayingProgress;
	}
	/**
	 * @param nowPlayingProgress the nowPlayingProgress to set
	 */
	public void setNowPlayingProgress(int nowPlayingProgress) {
		this.nowPlayingProgress = nowPlayingProgress;
	}
	/**
	 * @return the savedTracks
	 */
	public int[] getSavedTracks() {
		return savedTracks;
	}
	/**
	 * @param savedTracks the savedTracks to set
	 */
	public void setSavedTracks(int[] savedTracks) {
		this.savedTracks = savedTracks;
	}
}
